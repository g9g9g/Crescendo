# domain/services/difficulty_evaluator.py
"""
MusicXML 난이도 평가 서비스
"""

import logging
from typing import Dict, List, Optional
from music21 import converter, stream, note, chord, tempo, meter, key

logger = logging.getLogger(__name__)


class DifficultyEvaluator:
    """
    MusicXML 파일의 연주 난이도를 평가하는 서비스

    평가 기준:
    - 1-9 레벨 (1: 매우 쉬움, 9: 매우 어려움)
    - 6가지 평가 지표: 템포, 리듬 복잡도, 음정 도약, 화성 복잡도, 기술적 난이도, 곡 길이
    """

    # 레벨 정의 (브론즈-실버-골드 티어)
    LEVEL_NAMES = {
        1: "브론즈 3",
        2: "브론즈 2",
        3: "브론즈 1",
        4: "실버 3",
        5: "실버 2",
        6: "실버 1",
        7: "골드 3",
        8: "골드 2",
        9: "골드 1"
    }

    # 평가 지표 가중치 (총합 1.0)
    WEIGHTS = {
        'tempo': 0.20,          # 템포
        'rhythm': 0.20,         # 리듬 복잡도
        'intervals': 0.15,      # 음정 도약
        'harmony': 0.15,        # 화성 복잡도
        'technique': 0.20,      # 기술적 난이도
        'length': 0.10          # 곡 길이
    }

    def __init__(self):
        """초기화"""
        logger.info("DifficultyEvaluator initialized")

    def evaluate(self, xml_path: str) -> Dict:
        """
        MusicXML 파일의 난이도를 평가

        Args:
            xml_path: MusicXML 파일 경로

        Returns:
            평가 결과 딕셔너리
            {
                'success': bool,
                'level': int (1-9),
                'level_name': str,
                'total_score': float,
                'metrics': {...},
                'summary': str,
                'recommendations': [str, ...]
            }
        """
        try:
            # MusicXML 파싱
            logger.info(f"Parsing MusicXML: {xml_path}")
            score = converter.parse(xml_path)

            # 각 지표 평가
            metrics = self._evaluate_metrics(score)

            # 각 지표 로깅
            logger.info("=" * 50)
            logger.info("난이도 평가 지표 (각 0-10점):")
            for key, value in metrics.items():
                logger.info(f"  - {key}: {value:.1f}/10")
            logger.info("=" * 50)

            # 총 점수 계산 (가중 평균)
            total_score = sum(
                metrics[key] * self.WEIGHTS[key]
                for key in self.WEIGHTS.keys()
            ) * 10  # 0-100 스케일로 변환

            logger.info(f"총점: {total_score:.1f}/100 (가중 평균)")

            # 레벨 결정 (1-9)
            level = self._calculate_level(total_score)
            logger.info(f"최종 레벨: {level} - {self.LEVEL_NAMES[level]}")

            # 요약 생성
            summary = self._generate_summary(level, metrics, score)

            # 추천사항 생성
            recommendations = self._generate_recommendations(metrics)

            return {
                'success': True,
                'level': level,
                'level_name': self.LEVEL_NAMES[level],
                'total_score': round(total_score, 1),
                'metrics': {k: round(v, 1) for k, v in metrics.items()},
                'summary': summary,
                'recommendations': recommendations
            }

        except Exception as e:
            logger.error(f"Evaluation failed: {e}", exc_info=True)
            return {
                'success': False,
                'error': str(e)
            }

    def _evaluate_metrics(self, score: stream.Score) -> Dict[str, float]:
        """
        각 평가 지표를 계산

        Returns:
            각 지표별 점수 (0-10)
        """
        return {
            'tempo': self._evaluate_tempo(score),
            'rhythm': self._evaluate_rhythm(score),
            'intervals': self._evaluate_intervals(score),
            'harmony': self._evaluate_harmony(score),
            'technique': self._evaluate_technique(score),
            'length': self._evaluate_length(score)
        }

    def _evaluate_range(self, score: stream.Score) -> float:
        """
        음역 범위 평가 (0-10)

        넓은 음역 = 더 어려움
        """
        try:
            notes_and_chords = score.flatten().notesAndRests.stream()
            pitches = []

            for element in notes_and_chords:
                if isinstance(element, note.Note):
                    pitches.append(element.pitch.midi)
                elif isinstance(element, chord.Chord):
                    pitches.extend([p.midi for p in element.pitches])

            if not pitches:
                return 0.0

            # 음역 범위 (반음 수)
            pitch_range = max(pitches) - min(pitches)

            # 연속적 공식 (0 반음 = 0점, 60 반음(5옥타브) = 10점)
            # 더 부드러운 점수 변화
            range_score = min(10.0, pitch_range / 6.0)

            return round(range_score, 1)

        except Exception as e:
            logger.warning(f"Range evaluation failed: {e}")
            return 5.0

    def _evaluate_tempo(self, score: stream.Score) -> float:
        """
        템포 평가 (0-10)

        빠른 템포 = 더 어려움
        """
        try:
            tempo_marks = score.flatten().getElementsByClass(tempo.MetronomeMark)

            if not tempo_marks:
                logger.info("[Tempo] 템포 마크 없음 - 기본값 5.0")
                return 5.0

            # 평균 템포 계산
            avg_tempo = sum(t.number for t in tempo_marks) / len(tempo_marks)

            # 템포별 점수
            if avg_tempo < 60:
                score = 2.0
            elif avg_tempo < 90:
                score = 4.0
            elif avg_tempo < 120:
                score = 6.0
            elif avg_tempo < 150:
                score = 7.5
            else:
                score = 9.5

            logger.info(f"[Tempo] BPM: {avg_tempo:.1f} -> 점수: {score:.1f}/10")
            return score

        except Exception as e:
            logger.warning(f"[Tempo] 평가 실패: {e}")
            return 5.0

    def _evaluate_rhythm(self, score: stream.Score) -> float:
        """
        리듬 복잡도 평가 (0-10)

        복잡한 리듬 = 더 어려움
        """
        try:
            notes_and_rests = score.flatten().notesAndRests

            # 음표 길이 종류 수집
            durations = set()
            for n in notes_and_rests:
                durations.add(n.duration.quarterLength)

            # 복잡도 계산
            unique_rhythms = len(durations)

            # 복잡도별 점수
            if unique_rhythms <= 2:
                score = 2.0
            elif unique_rhythms <= 4:
                score = 4.0
            elif unique_rhythms <= 6:
                score = 6.0
            elif unique_rhythms <= 8:
                score = 8.0
            else:
                score = 10.0

            logger.info(f"[Rhythm] 고유 리듬 패턴: {unique_rhythms}개 -> 점수: {score:.1f}/10")
            return score

        except Exception as e:
            logger.warning(f"[Rhythm] 평가 실패: {e}")
            return 5.0

    def _evaluate_intervals(self, score: stream.Score) -> float:
        """
        음정 변화 평가 (0-10)

        큰 음정 변화 = 더 어려움
        """
        try:
            notes_stream = score.flatten().notes.stream()
            intervals = []

            prev_pitch = None
            for element in notes_stream:
                if isinstance(element, note.Note):
                    if prev_pitch is not None:
                        interval = abs(element.pitch.midi - prev_pitch)
                        intervals.append(interval)
                    prev_pitch = element.pitch.midi
                elif isinstance(element, chord.Chord):
                    # 코드의 최고음 사용
                    current_pitch = max(p.midi for p in element.pitches)
                    if prev_pitch is not None:
                        interval = abs(current_pitch - prev_pitch)
                        intervals.append(interval)
                    prev_pitch = current_pitch

            if not intervals:
                logger.info("[Intervals] 음정 변화 없음 - 0.0 반환")
                return 0.0

            # 평균 음정 변화
            avg_interval = sum(intervals) / len(intervals)

            # 음정별 점수
            if avg_interval < 2:  # 2도 미만
                score = 2.0
            elif avg_interval < 3:  # 3도 미만
                score = 4.0
            elif avg_interval < 5:  # 5도 미만
                score = 6.0
            elif avg_interval < 7:  # 7도 미만
                score = 8.0
            else:  # 옥타브 이상
                score = 10.0

            logger.info(f"[Intervals] 평균 음정 변화: {avg_interval:.1f} 반음 -> 점수: {score:.1f}/10")
            return score

        except Exception as e:
            logger.warning(f"[Intervals] 평가 실패: {e}")
            return 5.0

    def _evaluate_hand_span(self, score: stream.Score) -> float:
        """
        손 벌림 평가 (0-10) - 피아노용

        넓은 손 벌림 = 더 어려움
        """
        try:
            chords_stream = score.flatten().getElementsByClass(chord.Chord)

            if not chords_stream:
                # 화음 없음(단선율)은 중립 점수 (난이도와 무관)
                return 5.0

            max_span = 0
            for c in chords_stream:
                pitches = [p.midi for p in c.pitches]
                if len(pitches) >= 2:
                    span = max(pitches) - min(pitches)
                    max_span = max(max_span, span)

            # 손 벌림별 점수 (반음 수)
            if max_span < 7:  # 5도 미만
                return 2.0
            elif max_span < 12:  # 옥타브 미만
                return 4.0
            elif max_span < 15:  # 10도 미만
                return 6.0
            elif max_span < 19:  # 12도 미만
                return 8.0
            else:  # 옥타브 이상
                return 10.0

        except Exception as e:
            logger.warning(f"Hand span evaluation failed: {e}")
            return 5.0

    def _evaluate_accidentals(self, score: stream.Score) -> float:
        """
        임시표 평가 (0-10)

        많은 임시표 = 더 어려움
        """
        try:
            notes_stream = score.flatten().notes.stream()
            total_notes = 0
            accidental_notes = 0

            # 조성 확인
            key_sig = score.flatten().getElementsByClass(key.KeySignature)
            key_sharps_flats = set()
            if key_sig:
                key_sharps_flats = set(key_sig[0].alteredPitches)

            for element in notes_stream:
                if isinstance(element, note.Note):
                    total_notes += 1
                    # 조성에 없는 임시표인 경우
                    if element.pitch.accidental is not None:
                        if element.pitch not in key_sharps_flats:
                            accidental_notes += 1
                elif isinstance(element, chord.Chord):
                    for p in element.pitches:
                        total_notes += 1
                        if p.accidental is not None:
                            if p not in key_sharps_flats:
                                accidental_notes += 1

            if total_notes == 0:
                return 0.0

            # 임시표 비율
            accidental_ratio = accidental_notes / total_notes

            # 비율별 점수
            if accidental_ratio < 0.05:
                return 2.0
            elif accidental_ratio < 0.10:
                return 4.0
            elif accidental_ratio < 0.15:
                return 6.0
            elif accidental_ratio < 0.20:
                return 8.0
            else:
                return 10.0

        except Exception as e:
            logger.warning(f"Accidentals evaluation failed: {e}")
            return 5.0

    def _evaluate_harmony(self, score: stream.Score) -> float:
        """
        화성 복잡도 평가 (0-10)

        복잡한 화성 = 더 어려움 (음 개수 + 불협화음 고려)
        """
        try:
            chords_stream = score.flatten().getElementsByClass(chord.Chord)

            if not chords_stream:
                # 코드가 없으면 단선율로 간주 (낮은 난이도)
                return 2.0

            # 코드 분석
            chord_complexities = []

            for c in chords_stream:
                pitches = sorted([p.midi for p in c.pitches])
                num_notes = len(pitches)

                # 1. 음 개수에 따른 기본 복잡도
                if num_notes <= 2:
                    base_complexity = 2.0  # 단순 (2음)
                elif num_notes == 3:
                    base_complexity = 4.0  # 3화음
                elif num_notes == 4:
                    base_complexity = 6.0  # 7화음
                elif num_notes == 5:
                    base_complexity = 8.0  # 9화음
                else:
                    base_complexity = 10.0  # 복잡한 화음

                # 2. 불협화음 가산점 (반음/증음정 포함 시)
                dissonance_bonus = 0
                if num_notes >= 2:
                    for i in range(len(pitches) - 1):
                        interval = pitches[i+1] - pitches[i]
                        if interval == 1:  # 반음 (매우 불협화)
                            dissonance_bonus += 2
                        elif interval == 6:  # 증4도 (불협화)
                            dissonance_bonus += 1

                # 총 복잡도 (최대 10점)
                complexity = min(10.0, base_complexity + dissonance_bonus)
                chord_complexities.append(complexity)

            # 평균 화성 복잡도
            if chord_complexities:
                avg_complexity = sum(chord_complexities) / len(chord_complexities)
                final_score = min(avg_complexity, 10.0)
                logger.info(f"[Harmony] 코드 수: {len(chords_stream)}개, 평균 복잡도: {avg_complexity:.1f} -> 점수: {final_score:.1f}/10")
                return final_score
            else:
                logger.info("[Harmony] 코드 없음 - 2.0 반환")
                return 2.0

        except Exception as e:
            logger.warning(f"[Harmony] 평가 실패: {e}")
            return 5.0

    def _evaluate_technique(self, score: stream.Score) -> float:
        """
        기술적 난이도 평가 (0-10)

        음역, 손 벌림, 임시표를 종합적으로 고려
        """
        try:
            # 3가지 요소 평가
            range_score = self._evaluate_range(score)
            hand_span_score = self._evaluate_hand_span(score)
            accidentals_score = self._evaluate_accidentals(score)

            # 가중 평균 (음역 40%, 손 벌림 30%, 임시표 30%)
            technique_score = (
                range_score * 0.4 +
                hand_span_score * 0.3 +
                accidentals_score * 0.3
            )

            logger.info(f"[Technique] 음역:{range_score:.1f} + 손벌림:{hand_span_score:.1f} + 임시표:{accidentals_score:.1f} -> 점수: {technique_score:.1f}/10")
            return round(technique_score, 1)

        except Exception as e:
            logger.warning(f"[Technique] 평가 실패: {e}")
            return 5.0

    def _evaluate_length(self, score: stream.Score) -> float:
        """
        곡 길이 평가 (0-10)

        마디 수와 예상 연주 시간 기반
        """
        try:
            # 마디 수 계산 (여러 방법 시도)
            from music21 import stream as m21_stream

            # 방법 1: getElementsByClass('Measure')
            measures = list(score.flatten().getElementsByClass('Measure'))
            measure_count = len(measures)

            logger.info(f"[Length] 마디 수 (방법1): {measure_count}개")

            # 방법 2: parts에서 마디 계산
            if measure_count == 0:
                try:
                    parts = score.parts
                    if parts:
                        measures_in_part = list(parts[0].getElementsByClass('Measure'))
                        measure_count = len(measures_in_part)
                        logger.info(f"[Length] 마디 수 (방법2 - parts): {measure_count}개")
                except:
                    pass

            # 마디가 없으면 duration 기반 추정
            if measure_count == 0:
                try:
                    total_quarter_length = score.duration.quarterLength
                    if total_quarter_length > 0:
                        # 4/4 박자 가정: 1마디 = 4 quarter notes
                        measure_count = int(total_quarter_length / 4)
                        logger.info(f"[Length] 마디 수 (duration 추정): {measure_count}개")
                except:
                    pass

            if measure_count == 0:
                logger.warning("[Length] 마디 수를 찾을 수 없음 - 기본값 5.0 반환")
                return 5.0

            # ===== 1. 마디 수 기반 점수 (60점) =====
            if measure_count <= 8:
                measure_score = measure_count * 3  # 8마디: 24점
            elif measure_count <= 16:
                measure_score = 24 + (measure_count - 8) * 2  # 16마디: 40점
            elif measure_count <= 32:
                measure_score = 40 + (measure_count - 16) * 1  # 32마디: 56점
            elif measure_count <= 64:
                measure_score = 56 + (measure_count - 32) * 0.125  # 64마디: 60점
            else:
                measure_score = 60  # 64마디 이상: 최대 60점

            logger.info(f"[Length] 마디 점수: {measure_score:.1f}/60")

            # ===== 2. 예상 연주 시간 (40점) =====
            try:
                tempo_marks = score.flatten().getElementsByClass(tempo.MetronomeMark)
                if tempo_marks:
                    bpm = tempo_marks[0].number
                else:
                    bpm = 120  # 기본 템포

                # 4/4 박자 가정, 1마디 = 4박
                beats_total = measure_count * 4
                minutes = beats_total / bpm

                logger.info(f"[Length] 예상 시간: {minutes:.1f}분 (BPM: {bpm})")

                # 시간 기반 점수
                if minutes <= 1:
                    time_score = minutes * 10  # 1분: 10점
                elif minutes <= 2:
                    time_score = 10 + (minutes - 1) * 10  # 2분: 20점
                elif minutes <= 3:
                    time_score = 20 + (minutes - 2) * 7.5  # 3분: 27.5점
                elif minutes <= 5:
                    time_score = 27.5 + (minutes - 3) * 5  # 5분: 37.5점
                else:
                    time_score = min(40, 37.5 + (minutes - 5) * 1.25)  # 최대 40점

                logger.info(f"[Length] 시간 점수: {time_score:.1f}/40")

            except Exception as e:
                logger.warning(f"[Length] 시간 계산 오류: {e}")
                time_score = 20  # 기본값

            # ===== 총점 (0-100 -> 0-10 변환) =====
            total_score = (measure_score + time_score) / 10  # 100점 만점 -> 10점 만점

            logger.info(f"[Length] 최종 점수: {total_score:.1f}/10")

            # 최소 점수 보장
            return max(total_score, 0.5)

        except Exception as e:
            logger.error(f"[Length] 곡 길이 평가 실패: {e}", exc_info=True)
            return 5.0

    def _calculate_level(self, total_score: float) -> int:
        """
        총 점수를 1-9 레벨로 변환

        Args:
            total_score: 0-100 점수

        Returns:
            1-9 레벨
        """
        # 더 명확한 10점 단위 구간 (반올림으로 경계 부드럽게)
        if total_score <= 10:
            return 1
        elif total_score <= 20:
            return 2
        elif total_score <= 30:
            return 3
        elif total_score <= 40:
            return 4
        elif total_score <= 50:
            return 5
        elif total_score <= 60:
            return 6
        elif total_score <= 70:
            return 7
        elif total_score <= 80:
            return 8
        else:
            return 9

    def _generate_summary(
        self,
        level: int,
        metrics: Dict[str, float],
        score: stream.Score
    ) -> str:
        """
        평가 요약 생성

        Returns:
            요약 텍스트
        """
        level_name = self.LEVEL_NAMES[level]

        # 가장 높은 지표 찾기
        max_metric = max(metrics.items(), key=lambda x: x[1])

        metric_names = {
            'tempo': '템포가',
            'rhythm': '리듬 복잡도가',
            'intervals': '음정 도약이',
            'harmony': '화성 복잡도가',
            'technique': '기술적 난이도가',
            'length': '곡 길이가'
        }

        summary = f"{level_name} 수준의 곡입니다. "
        summary += f"특히 {metric_names[max_metric[0]]} 높은 편입니다."

        return summary

    def _generate_recommendations(self, metrics: Dict[str, float]) -> List[str]:
        """
        연습 추천사항 생성

        Returns:
            추천사항 리스트
        """
        recommendations = []

        # 높은 지표에 대한 연습 조언
        if metrics['tempo'] >= 7:
            recommendations.append("빠른 템포: 메트로놈을 사용하여 천천히 속도를 올려가며 연습하세요.")

        if metrics['rhythm'] >= 7:
            recommendations.append("복잡한 리듬: 리듬 패턴을 따로 연습하고 박자를 정확히 세며 연주하세요.")

        if metrics['intervals'] >= 7:
            recommendations.append("큰 음정 도약: 도약 연습과 음정 감각 훈련이 필요합니다.")

        if metrics['harmony'] >= 7:
            recommendations.append("복잡한 화성: 코드 구조를 분석하고 각 음의 역할을 이해하며 연습하세요.")

        if metrics['technique'] >= 7:
            recommendations.append("높은 기술적 난이도: 손가락 독립성 운동, 스트레칭, 스케일 연습을 충분히 하세요.")

        if metrics['length'] >= 7:
            recommendations.append("긴 곡: 구간별로 나누어 연습하고, 체력 관리와 집중력 유지에 신경 쓰세요.")

        # 기본 추천사항
        if not recommendations:
            recommendations.append("전체적으로 균형 잡힌 곡입니다. 꾸준한 연습을 권장합니다.")

        return recommendations
