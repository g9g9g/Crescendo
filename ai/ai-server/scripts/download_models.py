# scripts/download_models.py
"""
프로덕션 배포 전 모델 다운로드 스크립트
"""

import os
import sys
from piano_transcription_inference import PianoTranscription

def download_piano_transcription_model(models_dir: str, device: str = 'cpu'):
    """
    piano_transcription 모델 다운로드
    
    Args:
        models_dir: 모델 저장 디렉토리
        device: 'cpu' or 'cuda'
    """
    print("="*60)
    print("🎹 Piano Transcription 모델 다운로드 시작")
    print("="*60)
    
    os.makedirs(models_dir, exist_ok=True)
    
    try:
        # 모델 초기화 (자동으로 다운로드됨)
        print(f"\n📦 모델 다운로드 중... (저장 위치: {models_dir})")
        
        model = PianoTranscription(device=device)
        
        # 체크포인트 경로 확인
        checkpoint_dir = os.path.join(models_dir, 'piano_transcription')
        os.makedirs(checkpoint_dir, exist_ok=True)
        
        print(f"\n✓ 모델 다운로드 완료!")
        print(f"   저장 위치: {checkpoint_dir}")
        
        # 모델 정보 출력
        print(f"\n📊 모델 정보:")
        print(f"   Device: {device}")
        print(f"   Model type: Regress_onset_offset_frame_velocity_CRNN")
        
        return True
        
    except Exception as e:
        print(f"\n❌ 모델 다운로드 실패: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == '__main__':
    # 환경변수 또는 기본값 사용
    models_dir = os.getenv('MODELS_DIR', './models')
    device = os.getenv('TORCH_DEVICE', 'cpu')
    
    print(f"Models directory: {models_dir}")
    print(f"Device: {device}\n")
    
    success = download_piano_transcription_model(models_dir, device)
    
    sys.exit(0 if success else 1)