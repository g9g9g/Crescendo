# domain/strategies/evaluation_metrics.py
"""
연주 평가 메트릭 전략 패턴
"""

import numpy as np
from typing import Dict, List
from abc import ABC, abstractmethod


class EvaluationMetric(ABC):
    """평가 메트릭 추상 클래스"""

    @abstractmethod
    def calculate(self, notes: List[Dict]) -> Dict:
        """
        메트릭 계산

        Args:
            notes: [{start, end, duration, pitch, velocity}, ...]

        Returns:
            {'name': str, 'score': float}
        """
        pass


class TempoStabilityMetric(EvaluationMetric):
    """템포 안정성 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if len(notes) < 2:
            return {'name': '템포 안정성', 'score': 50.0}

        # 연속된 노트 간격 계산
        intervals = []
        for i in range(len(notes) - 1):
            interval = notes[i+1]['start'] - notes[i]['start']
            if interval > 0:
                intervals.append(interval)

        if not intervals:
            return {'name': '템포 안정성', 'score': 50.0}

        # 간격의 표준편차로 안정성 평가 (낮을수록 안정적)
        std_dev = np.std(intervals)
        mean_interval = np.mean(intervals)

        if mean_interval == 0:
            return {'name': '템포 안정성', 'score': 50.0}

        # 변동계수 (CV) 계산
        cv = std_dev / mean_interval

        # CV를 0~100 점수로 변환 (0.5 이하면 만점, 더 관대하게)
        score = max(0, min(100, 100 - cv * 100))

        return {'name': '템포 안정성', 'score': score}


class RhythmConsistencyMetric(EvaluationMetric):
    """리듬 일관성 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if len(notes) < 2:
            return {'name': '리듬 일관성', 'score': 50.0}

        # 음표 길이의 일관성 평가
        durations = [n['duration'] for n in notes if n['duration'] > 0]

        if not durations:
            return {'name': '리듬 일관성', 'score': 50.0}

        # 길이별 그룹화 (0.1초 단위)
        duration_groups = {}
        for d in durations:
            key = round(d, 1)
            duration_groups[key] = duration_groups.get(key, 0) + 1

        # 주요 리듬 패턴이 많을수록 높은 점수
        total = len(durations)
        top_patterns = sorted(duration_groups.values(), reverse=True)[:3]
        pattern_ratio = sum(top_patterns) / total

        score = pattern_ratio * 100

        return {'name': '리듬 일관성', 'score': score}


class DynamicsExpressionMetric(EvaluationMetric):
    """다이나믹 표현 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if not notes:
            return {'name': '다이나믹 표현', 'score': 50.0}

        velocities = [n['velocity'] for n in notes]

        # 벨로시티 범위로 표현력 평가
        velocity_range = max(velocities) - min(velocities)

        # 변화 빈도도 고려 (표현력의 중요 요소)
        if len(velocities) > 1:
            velocity_changes = [abs(velocities[i+1] - velocities[i])
                              for i in range(len(velocities)-1)]
            avg_change = np.mean(velocity_changes)
        else:
            avg_change = 0

        # 범위 점수 (30~100 범위가 이상적)
        if velocity_range < 20:
            range_score = velocity_range * 2  # 너무 단조로움
        elif velocity_range > 100:
            range_score = max(0, 100 - (velocity_range - 100) * 0.5)  # 너무 극단적
        else:
            range_score = 60 + (velocity_range - 20) * 0.5

        # 변화 빈도 점수 (평균 5 이상이 좋음)
        change_score = min(100, avg_change * 10)

        # 범위와 변화 빈도를 모두 고려 (70% 범위, 30% 변화)
        score = range_score * 0.7 + change_score * 0.3

        return {'name': '다이나믹 표현', 'score': min(100, score)}


class ArticulationBalanceMetric(EvaluationMetric):
    """아티큘레이션 균형 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if not notes:
            return {'name': '아티큘레이션 균형', 'score': 50.0}

        durations = [n['duration'] for n in notes if n['duration'] > 0]

        if not durations:
            return {'name': '아티큘레이션 균형', 'score': 50.0}

        # 짧은 음(0.3초 이하)과 긴 음(0.5초 이상)의 비율
        short_notes = sum(1 for d in durations if d < 0.3)
        long_notes = sum(1 for d in durations if d > 0.5)
        total = len(durations)

        short_ratio = short_notes / total
        long_ratio = long_notes / total

        # 균형있게 섞여있으면 높은 점수
        balance = 1 - abs(short_ratio - long_ratio)
        score = balance * 100

        return {'name': '아티큘레이션 균형', 'score': score}


class CleanTechniqueMetric(EvaluationMetric):
    """클린 테크닉 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if len(notes) < 2:
            return {'name': '클린 테크닉', 'score': 50.0}

        # 매우 짧은 음표나 중복 음표를 에러로 간주
        very_short_notes = sum(1 for n in notes if n['duration'] < 0.05)

        # 동시에 너무 많은 음이 연주되는 경우
        overlaps = 0
        for i, note in enumerate(notes):
            concurrent = sum(1 for other in notes[i+1:]
                           if other['start'] < note['end'])
            if concurrent > 10:  # 10개 이상 동시 연주는 비정상적 (화음 고려)
                overlaps += 1

        total = len(notes)
        error_ratio = (very_short_notes + overlaps) / total

        # 에러가 적을수록 높은 점수
        score = max(0, 100 - error_ratio * 200)

        return {'name': '클린 테크닉', 'score': score}


class PitchDiversityMetric(EvaluationMetric):
    """음높이 다양성 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if not notes:
            return {'name': '음높이 다양성', 'score': 50.0}

        pitches = [n['pitch'] for n in notes]
        unique_pitches = len(set(pitches))

        # 고유 음높이 개수로 다양성 평가
        # 12개 이상이면 만점 (1옥타브)
        score = min(100, (unique_pitches / 12) * 100)

        return {'name': '음높이 다양성', 'score': score}


class PolyphonyControlMetric(EvaluationMetric):
    """폴리포니 컨트롤 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if not notes:
            return {'name': '폴리포니 컨트롤', 'score': 50.0}

        # 시간대별 동시 연주 음표 수 분석
        time_points = []
        for note in notes:
            time_points.append((note['start'], 1))  # 시작
            time_points.append((note['end'], -1))   # 종료

        time_points.sort()

        current_notes = 0
        max_polyphony = 0
        polyphony_values = []

        for _, change in time_points:
            current_notes += change
            if current_notes > 0:
                polyphony_values.append(current_notes)
            max_polyphony = max(max_polyphony, current_notes)

        if not polyphony_values:
            return {'name': '폴리포니 컨트롤', 'score': 50.0}

        avg_polyphony = np.mean(polyphony_values)

        # 2~4개 동시 연주가 이상적
        if 2 <= avg_polyphony <= 4:
            score = 100
        elif avg_polyphony < 2:
            score = avg_polyphony * 50
        else:
            score = max(0, 100 - (avg_polyphony - 4) * 10)

        return {'name': '폴리포니 컨트롤', 'score': min(100, score)}


class PhraseVarietyMetric(EvaluationMetric):
    """프레이즈 다양성 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if len(notes) < 4:
            return {'name': '프레이즈 다양성', 'score': 50.0}

        # 4음 단위로 프레이즈 패턴 분석
        phrase_patterns = []

        for i in range(len(notes) - 3):
            # 음높이 변화 패턴
            pattern = tuple(
                notes[i+j+1]['pitch'] - notes[i+j]['pitch']
                for j in range(3)
            )
            phrase_patterns.append(pattern)

        if not phrase_patterns:
            return {'name': '프레이즈 다양성', 'score': 50.0}

        # 고유 패턴 비율
        unique_patterns = len(set(phrase_patterns))
        total_patterns = len(phrase_patterns)

        diversity_ratio = unique_patterns / total_patterns
        score = diversity_ratio * 100

        return {'name': '프레이즈 다양성', 'score': score}


class PacingBalanceMetric(EvaluationMetric):
    """페이싱 균형 평가"""

    def calculate(self, notes: List[Dict]) -> Dict:
        if len(notes) < 10:
            return {'name': '페이싱 균형', 'score': 50.0}

        # 전체 연주를 3등분하여 각 구간의 밀도 분석
        total_duration = notes[-1]['end']
        section_size = total_duration / 3

        section_counts = [0, 0, 0]
        for note in notes:
            section_idx = min(2, int(note['start'] / section_size))
            section_counts[section_idx] += 1

        # 각 구간의 밀도 차이 계산
        avg_density = np.mean(section_counts)
        if avg_density == 0:
            return {'name': '페이싱 균형', 'score': 50.0}

        density_variance = np.var(section_counts) / (avg_density ** 2)

        # 변동이 적을수록 균형잡힘
        score = max(0, 100 - density_variance * 100)

        return {'name': '페이싱 균형', 'score': min(100, score)}


class MetricFactory:
    """메트릭 팩토리"""

    _metrics = {
        'tempo_stability': TempoStabilityMetric(),
        'rhythm_consistency': RhythmConsistencyMetric(),
        'dynamics_expression': DynamicsExpressionMetric(),
        'articulation_balance': ArticulationBalanceMetric(),
        'clean_technique': CleanTechniqueMetric(),
        'pitch_diversity': PitchDiversityMetric(),
        'polyphony_control': PolyphonyControlMetric(),
        'phrase_variety': PhraseVarietyMetric(),
        'pacing_balance': PacingBalanceMetric(),
    }

    @classmethod
    def get_metric(cls, metric_name: str) -> EvaluationMetric:
        """
        메트릭 인스턴스 반환

        Args:
            metric_name: 메트릭 이름

        Returns:
            EvaluationMetric 인스턴스

        Raises:
            ValueError: 알 수 없는 메트릭 이름
        """
        metric = cls._metrics.get(metric_name)
        if metric is None:
            raise ValueError(f"Unknown metric: {metric_name}")
        return metric
