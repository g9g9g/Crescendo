# domain/services/style_transfer_service.py
"""
MusicXML 장르 스타일 변환 서비스 (규칙 기반)
"""

import logging
from typing import Dict
import random

logger = logging.getLogger(__name__)


class StyleTransferService:
    """규칙 기반 스타일 변환 서비스"""

    SUPPORTED_STYLES = ['jazz', 'classical', 'pop', 'bossa_nova', 'waltz', 'swing', 'baroque']

    def __init__(self):
        """규칙 기반 편곡 서비스 초기화"""
        logger.info("Initializing rule-based style transfer service")
        self.ai_available = False  # 호환성을 위해 유지

    def convert_style(self, input_xml_path: str, style: str, output_xml_path: str) -> Dict:
        """
        규칙 기반 스타일 변환 실행

        Args:
            input_xml_path: 입력 MusicXML 파일 경로
            style: 타겟 장르
            output_xml_path: 출력 MusicXML 파일 경로

        Returns:
            변환 결과 딕셔너리
        """
        try:
            logger.info(f"Starting rule-based style transfer: {style}")

            # 장르 검증
            if style not in self.SUPPORTED_STYLES:
                return {
                    'success': False,
                    'error': f'Unsupported style: {style}',
                    'supported_styles': self.SUPPORTED_STYLES
                }

            # 1. MusicXML 파싱
            logger.info("Parsing MusicXML...")
            from music21 import converter
            score = converter.parse(input_xml_path)

            # 2. 템포 및 박자 설정
            logger.info("Setting tempo and time signature...")
            score = self._set_tempo_and_meter(score, style)

            # 3. 다이나믹스 조정
            logger.info("Adjusting dynamics...")
            score = self._adjust_dynamics(score, style)

            # 4. 아티큘레이션 적용
            logger.info("Applying articulation...")
            score = self._apply_articulation(score, style)

            # 5. 화음 변형
            logger.info("Transforming chords...")
            score = self._transform_chords(score, style)

            # 6. 리듬 패턴 변형
            logger.info("Transforming rhythm patterns...")
            score = self._transform_rhythm(score, style)

            # 7. 장식음 추가
            logger.info("Adding ornamentations...")
            score = self._add_ornamentations(score, style)

            # 8. 저장
            logger.info("Saving transformed score...")
            score.write('musicxml', fp=output_xml_path)

            return {
                'success': True,
                'output_path': output_xml_path,
                'style': style,
                'method': 'rule-based'
            }

        except Exception as e:
            logger.error(f"Style transfer failed: {e}", exc_info=True)
            return {
                'success': False,
                'error': str(e)
            }

    def _set_tempo_and_meter(self, score, style):
        """템포와 박자 설정 (극단적)"""
        from music21 import tempo, meter, stream

        # 템포 매핑 (극단적으로 차이 나게)
        tempo_map = {
            'jazz': 180,        # 매우 빠름
            'classical': 72,    # 매우 느림
            'pop': 128,         # 중간
            'bossa_nova': 120,  # 느긋함
            'waltz': 60,        # 매우 느림
            'swing': 200,       # 극도로 빠름
            'baroque': 96       # 약간 느림
        }

        # 박자 매핑
        meter_map = {
            'waltz': '3/4',
            'swing': '4/4',
            'jazz': '4/4',
            'classical': '4/4',
            'pop': '4/4',
            'bossa_nova': '4/4',
            'baroque': '4/4'
        }

        target_tempo = tempo_map.get(style, 120)
        target_meter = meter_map.get(style, '4/4')

        for part in score.parts:
            measures = part.getElementsByClass(stream.Measure)
            if measures:
                first_measure = measures[0]
                # 템포 설정
                first_measure.insert(0, tempo.MetronomeMark(number=target_tempo))
                # 박자 설정
                if style == 'waltz':
                    first_measure.timeSignature = meter.TimeSignature(target_meter)

        logger.info(f"Set tempo: {target_tempo} BPM, meter: {target_meter}")
        return score

    def _adjust_dynamics(self, score, style):
        """다이나믹스 조정 (극단적)"""
        from music21 import dynamics, note, chord, stream, volume

        # 장르별 다이나믹스 범위와 velocity 매핑 (극단적으로)
        dynamics_map = {
            'jazz': {'marks': ['f', 'ff'], 'velocity_range': (95, 115), 'variation': 20},  # 매우 강하고 변화 큼
            'classical': {'marks': ['pp', 'p', 'mp'], 'velocity_range': (40, 70), 'variation': 15},  # 매우 부드럽고 섬세
            'pop': {'marks': ['f'], 'velocity_range': (100, 110), 'variation': 5},  # 일정하게 강함
            'bossa_nova': {'marks': ['mp', 'p'], 'velocity_range': (50, 65), 'variation': 8},  # 매우 부드럽고 일정
            'waltz': {'marks': ['mf', 'f'], 'velocity_range': (70, 100), 'variation': 25},  # 첫 박자 강조
            'swing': {'marks': ['ff', 'fff'], 'velocity_range': (105, 127), 'variation': 15},  # 극도로 강함
            'baroque': {'marks': ['mp', 'mf'], 'velocity_range': (65, 80), 'variation': 10}  # 중간 세기, 테라스 다이나믹스
        }

        style_config = dynamics_map.get(style, {'marks': ['mf'], 'velocity_range': (80, 90), 'variation': 10})
        target_dynamics = style_config['marks']
        velocity_min, velocity_max = style_config['velocity_range']
        variation = style_config['variation']

        for part in score.parts:
            measures = part.getElementsByClass(stream.Measure)
            for measure in measures:
                elements = measure.notesAndRests
                for i, element in enumerate(elements):
                    if isinstance(element, (note.Note, chord.Chord)):
                        # 장르별 특별한 다이나믹스 패턴
                        if style == 'waltz':
                            # 왈츠: 첫 박자 매우 강하게, 2,3박자 약하게
                            if i % 3 == 0:
                                vel = velocity_max
                            else:
                                vel = velocity_min
                        elif style == 'pop':
                            # 팝: 매우 일정하게
                            vel = (velocity_min + velocity_max) // 2
                        elif style == 'swing':
                            # 스윙: 오프비트 강조
                            if i % 2 == 1:
                                vel = velocity_max
                            else:
                                vel = velocity_max - 10
                        else:
                            # 기타: 범위 내 랜덤
                            vel = random.randint(velocity_min, velocity_max)

                        element.volume.velocity = min(127, max(1, vel))

                        # 다이나믹 마크 더 자주 추가
                        if i == 0 and measure.number % 2 == 1:
                            dyn = dynamics.Dynamic(random.choice(target_dynamics))
                            measure.insert(element.offset, dyn)

        return score

    def _apply_articulation(self, score, style):
        """아티큘레이션 적용 (극단적)"""
        from music21 import articulations, note, chord

        articulation_map = {
            'jazz': [articulations.Accent, articulations.Staccato],
            'classical': [articulations.Tenuto],  # 레가토를 위한 테누토
            'pop': [articulations.Accent],
            'bossa_nova': [articulations.Tenuto],
            'waltz': [articulations.Accent],  # 첫 박자 액센트
            'swing': [articulations.Accent, articulations.Staccato],
            'baroque': [articulations.Staccato, articulations.Staccatissimo]
        }

        target_articulations = articulation_map.get(style, [])

        if not target_articulations:
            return score

        for part in score.parts:
            notes_and_chords = part.flatten().notesAndRests
            for i, element in enumerate(notes_and_chords):
                if isinstance(element, (note.Note, chord.Chord)):
                    # 장르별 극단적인 아티큘레이션 패턴
                    if style == 'jazz':
                        # 재즈: 모든 오프비트에 강한 액센트
                        if i % 2 == 1:
                            element.articulations.append(articulations.Accent())
                        if i % 4 == 3:
                            element.articulations.append(articulations.Staccato())

                    elif style == 'swing':
                        # 스윙: 거의 모든 음표에 액센트
                        element.articulations.append(articulations.Accent())
                        if i % 2 == 1:
                            element.articulations.append(articulations.Staccato())

                    elif style == 'classical':
                        # 클래식: 모든 음표에 레가토 (매우 부드럽게)
                        element.articulations.append(articulations.Tenuto())

                    elif style == 'baroque':
                        # 바로크: 대부분의 음표에 스타카토
                        if i % 2 == 0:
                            element.articulations.append(articulations.Staccato())
                        else:
                            element.articulations.append(articulations.Staccatissimo())

                    elif style == 'pop':
                        # 팝: 강한 백비트 액센트
                        if i % 4 in [1, 3]:  # 2,4박자
                            element.articulations.append(articulations.Accent())

                    elif style == 'waltz':
                        # 왈츠: 첫 박자만 강한 액센트
                        if i % 3 == 0:
                            element.articulations.append(articulations.Accent())
                        else:
                            element.articulations.append(articulations.Tenuto())

                    elif style == 'bossa_nova':
                        # 보사노바: 부드러운 테누토
                        if i % 2 == 0:
                            element.articulations.append(articulations.Tenuto())

        return score

    def _transform_chords(self, score, style):
        """화음 변형 (극단적)"""
        from music21 import chord, pitch

        for part in score.parts:
            chords = part.flatten().getElementsByClass(chord.Chord)
            for c in chords:
                if style == 'jazz':
                    # 재즈: 매우 복잡한 확장 화음 (7, 9, 11, 13화음)
                    self._add_jazz_extensions(c, extended=True)

                elif style == 'swing':
                    # 스윙: 재즈 화음 + 더 강한 확장
                    self._add_jazz_extensions(c, extended=True)

                elif style == 'classical':
                    # 클래식: 엄격한 3화음만
                    self._simplify_to_triad(c)

                elif style == 'pop':
                    # 팝: 매우 단순한 화음 (power chord 스타일)
                    self._simplify_to_power_chord(c)

                elif style == 'bossa_nova':
                    # 보사노바: 복잡한 재즈 화음
                    self._add_bossa_voicing(c)

                elif style == 'baroque':
                    # 바로크: 전통 3화음
                    self._simplify_to_triad(c)

                elif style == 'waltz':
                    # 왈츠: 단순한 3화음
                    self._simplify_to_triad(c)

        return score

    def _add_jazz_extensions(self, c, extended=False):
        """재즈 확장 화음 추가 (극단적)"""
        from music21 import pitch

        if len(c.pitches) >= 2:
            root = c.root()
            try:
                # 7화음 추가
                seventh = pitch.Pitch(root.name)
                seventh.octave = root.octave + 1
                seventh.transpose(10, inPlace=True)  # 단7도
                if seventh not in c.pitches:
                    c.add(seventh)

                if extended:
                    # 9화음 추가
                    ninth = pitch.Pitch(root.name)
                    ninth.octave = root.octave + 1
                    ninth.transpose(14, inPlace=True)  # 장9도
                    if ninth not in c.pitches:
                        c.add(ninth)

                    # 11화음 추가
                    if random.random() < 0.5:
                        eleventh = pitch.Pitch(root.name)
                        eleventh.octave = root.octave + 1
                        eleventh.transpose(17, inPlace=True)  # 완전11도
                        if eleventh not in c.pitches:
                            c.add(eleventh)
            except:
                pass

    def _simplify_to_triad(self, c):
        """3화음으로 단순화 (극단적)"""
        if len(c.pitches) > 3:
            # 가장 낮은 3개 음만 유지
            pitches = sorted(c.pitches, key=lambda p: p.ps)[:3]
            c.pitches = pitches
        elif len(c.pitches) < 3:
            # 음이 부족하면 완전5도 추가
            try:
                root = c.root()
                fifth = pitch.Pitch(root.name)
                fifth.transpose(7, inPlace=True)
                if fifth not in c.pitches:
                    c.add(fifth)
            except:
                pass

    def _simplify_to_power_chord(self, c):
        """Power chord로 단순화 (루트+5도만)"""
        from music21 import pitch

        try:
            root = c.root()
            fifth = pitch.Pitch(root.name)
            fifth.transpose(7, inPlace=True)
            # 루트와 5도만 남기기
            c.pitches = [root, fifth]
        except:
            pass

    def _add_bossa_voicing(self, c):
        """보사노바 보이싱 (복잡한 재즈 화음)"""
        self._add_jazz_extensions(c, extended=True)

    def _transform_rhythm(self, score, style):
        """리듬 패턴 변형"""
        from music21 import note, chord

        for part in score.parts:
            notes_and_chords = part.flatten().notesAndRests

            for element in notes_and_chords:
                if isinstance(element, (note.Note, chord.Chord)):
                    if style in ['jazz', 'swing']:
                        # 스윙 리듬 (셋잇단음표 느낌)
                        # duration 약간 조정
                        pass  # music21에서 스윙은 재생 시 적용
                    elif style == 'bossa_nova':
                        # 싱코페이션 강조
                        pass

        return score

    def _add_ornamentations(self, score, style):
        """장식음 추가 (극단적)"""
        from music21 import expressions, note

        ornamentation_map = {
            'jazz': {'ornaments': ['turn'], 'probability': 0.4},
            'classical': {'ornaments': ['trill', 'turn'], 'probability': 0.5},
            'baroque': {'ornaments': ['trill', 'mordent', 'turn'], 'probability': 0.8},  # 매우 많은 장식음
            'waltz': {'ornaments': ['turn'], 'probability': 0.3},
            'pop': {'ornaments': [], 'probability': 0.0},  # 장식음 없음
            'bossa_nova': {'ornaments': [], 'probability': 0.1},
            'swing': {'ornaments': ['turn'], 'probability': 0.3}
        }

        config = ornamentation_map.get(style, {'ornaments': [], 'probability': 0.0})
        ornaments = config['ornaments']
        probability = config['probability']

        if not ornaments:
            return score

        for part in score.parts:
            notes = part.flatten().getElementsByClass(note.Note)
            for i, n in enumerate(notes):
                # 장르별 장식음 조건
                if style == 'baroque':
                    # 바로크: 거의 모든 긴 음표에 장식음
                    if n.duration.quarterLength >= 0.5:
                        if random.random() < probability:
                            orn_type = random.choice(ornaments)
                            if orn_type == 'trill':
                                n.expressions.append(expressions.Trill())
                            elif orn_type == 'turn':
                                n.expressions.append(expressions.Turn())
                            elif orn_type == 'mordent':
                                n.expressions.append(expressions.Mordent())

                elif style == 'classical':
                    # 클래식: 긴 음표와 프레이즈 끝에 장식음
                    if n.duration.quarterLength >= 1.0 or (i > 0 and i % 8 == 7):
                        if random.random() < probability:
                            orn_type = random.choice(ornaments)
                            if orn_type == 'trill':
                                n.expressions.append(expressions.Trill())
                            elif orn_type == 'turn':
                                n.expressions.append(expressions.Turn())

                elif style in ['jazz', 'swing']:
                    # 재즈/스윙: 가끔 턴
                    if n.duration.quarterLength >= 1.0:
                        if random.random() < probability:
                            n.expressions.append(expressions.Turn())

                elif style == 'waltz':
                    # 왈츠: 프레이즈 끝에만
                    if i % 12 == 11 and n.duration.quarterLength >= 0.5:
                        if random.random() < probability:
                            n.expressions.append(expressions.Turn())

        return score
