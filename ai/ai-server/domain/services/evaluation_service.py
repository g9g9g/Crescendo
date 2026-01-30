# domain/services/evaluation_service.py
"""
연주 평가 비즈니스 로직 (모델 매니저 사용)
"""

import os
import tempfile
import numpy as np
import pretty_midi
from typing import Dict, List

from domain.strategies.evaluation_metrics import MetricFactory
from util.audio_loader import load_audio_file
from infra.model_manager import model_manager 


class PerformanceEvaluationService:
    """연주 평가 서비스 (싱글톤 모델 사용)"""
    
    METRIC_WEIGHTS = {
        'tempo_stability': 16,
        'rhythm_consistency': 12,
        'dynamics_expression': 12,
        'articulation_balance': 12,
        'clean_technique': 12,
        'pitch_diversity': 12,
        'polyphony_control': 8,
        'phrase_variety': 8,
        'pacing_balance': 8,
    }
    
    def __init__(self):
        """
        모델은 ModelManager에서 관리하므로 여기서는 초기화 불필요
        """
        pass
    
    def evaluate(
        self, 
        audio_path: str,
        return_midi: bool = False
    ) -> Dict:
        """
        오디오 파일 평가
        
        Args:
            audio_path: 오디오 파일 경로
            return_midi: MIDI 경로 반환 여부
        
        Returns:
            평가 결과 딕셔너리
        """
        try:
            # Step 1: 오디오 로드
            audio, sr = load_audio_file(audio_path, sample_rate=16000)
            
            # Step 2: 모델 가져오기 (싱글톤)
            transcriptor = model_manager.get_transcription_model()
            
            # Step 3: 전사 수행
            with tempfile.NamedTemporaryFile(suffix='.mid', delete=False) as tmp:
                midi_path = tmp.name
            
            transcriptor.transcribe(audio, midi_path)
            
            # Step 4: MIDI 파싱
            notes = self._parse_midi(midi_path)
            
            if len(notes) == 0:
                if not return_midi:
                    os.remove(midi_path)
                return {
                    'success': False,
                    'error': '감지된 노트가 없습니다.'
                }
            
            # Step 5: 평가 수행
            metrics = self._calculate_all_metrics(notes)
            overall_score = self._calculate_overall_score(metrics)
            feedback = self._generate_feedback(metrics, overall_score)
            
            result = {
                'success': True,
                'overall_score': round(overall_score, 2),
                'grade': self._get_grade(overall_score),
                'metrics': {
                    k: {'name': v['name'], 'score': round(v['score'], 2)}
                    for k, v in metrics.items()
                },
                'feedback': feedback,
                'stats': {
                    'total_notes': len(notes),
                    'duration': round(notes[-1]['end'], 2),
                    'avg_velocity': round(float(np.mean([n['velocity'] for n in notes])), 2)
                }
            }
            
            if return_midi:
                result['midi_path'] = midi_path
            else:
                os.remove(midi_path)
            
            return result
            
        except Exception as e:
            import traceback
            return {
                'success': False,
                'error': str(e),
                'traceback': traceback.format_exc()
            }
    
    # ... (나머지 메서드는 이전과 동일)
    
    def _parse_midi(self, midi_path: str) -> List[Dict]:
        """MIDI 파일 파싱"""
        midi_data = pretty_midi.PrettyMIDI(midi_path)
        
        notes = []
        for instrument in midi_data.instruments:
            if not instrument.is_drum:
                for note in instrument.notes:
                    notes.append({
                        'start': note.start,
                        'end': note.end,
                        'duration': note.end - note.start,
                        'pitch': note.pitch,
                        'velocity': note.velocity
                    })
        
        notes.sort(key=lambda x: x['start'])
        return notes
    
    def _calculate_all_metrics(self, notes: List[Dict]) -> Dict:
        """모든 지표 계산"""
        metrics = {}
        for metric_name in self.METRIC_WEIGHTS.keys():
            metric = MetricFactory.get_metric(metric_name)
            metrics[metric_name] = metric.calculate(notes)
        return metrics
    
    def _calculate_overall_score(self, metrics: Dict) -> float:
        """가중 평균 점수 계산"""
        return sum(
            metrics[name]['score'] * (weight / 100)
            for name, weight in self.METRIC_WEIGHTS.items()
        )
    
    def _generate_feedback(self, metrics: Dict, overall_score: float) -> List[str]:
        """피드백 생성"""
        feedback = []
        
        if overall_score >= 90:
            feedback.append("훌륭합니다! 매우 완성도 높은 연주입니다.")
        elif overall_score >= 80:
            feedback.append("좋은 연주입니다! 약간의 개선으로 더 완벽해질 수 있습니다.")
        elif overall_score >= 70:
            feedback.append("괜찮은 연주입니다. 몇 가지 부분을 보완하면 좋겠습니다.")
        elif overall_score >= 60:
            feedback.append("기본기는 갖춰져 있습니다. 연습을 통해 더 발전할 수 있습니다.")
        else:
            feedback.append("더 많은 연습이 필요합니다. 기본기부터 차근차근 다져보세요.")
        
        sorted_metrics = sorted(metrics.items(), key=lambda x: x[1]['score'], reverse=True)
        
        strengths = [m for m in sorted_metrics[:3] if m[1]['score'] >= 80]
        if strengths:
            feedback.append("\n강점:")
            for _, metric in strengths:
                feedback.append(f"  • {metric['name']}: {metric['score']:.1f}점")
        
        weaknesses = [m for m in sorted_metrics[-3:] if m[1]['score'] < 70]
        if weaknesses:
            feedback.append("\n개선 포인트:")
            for key, metric in weaknesses:
                suggestion = self._get_improvement_suggestion(key)
                feedback.append(f"  • {metric['name']}: {suggestion}")
        
        return feedback
    
    def _get_improvement_suggestion(self, metric_name: str) -> str:
        """개선 제안 메시지"""
        suggestions = {
            'tempo_stability': '박자를 좀 더 일정하게 유지해보세요.',
            'dynamics_expression': '강약 표현을 더 다양하게 해보세요.',
            'clean_technique': '불필요한 음이나 실수음을 줄여보세요.',
            'rhythm_consistency': '리듬 패턴을 더 명확하게 연주해보세요.',
            'articulation_balance': '짧은 음과 긴 음의 균형을 조절해보세요.',
            'pitch_diversity': '음역대를 더 다양하게 사용해보세요.',
            'polyphony_control': '화음 연주를 더 균형있게 해보세요.',
            'phrase_variety': '프레이즈 표현을 더 풍부하게 해보세요.',
            'pacing_balance': '곡 전체의 흐름을 고르게 분배해보세요.',
        }
        return suggestions.get(metric_name, '연습이 필요합니다.')
    
    def _get_grade(self, score: float) -> str:
        """등급 반환"""
        if score >= 95:
            return "S"
        elif score >= 90:
            return "A+"
        elif score >= 85:
            return "A"
        elif score >= 80:
            return "B+"
        elif score >= 75:
            return "B"
        elif score >= 70:
            return "C+"
        elif score >= 60:
            return "C"
        else:
            return "D"