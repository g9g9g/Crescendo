# infra/model_manager.py
"""
모델 관리 싱글톤
한 번만 로드하고 재사용
"""

import os
import logging
from typing import Optional
from piano_transcription_inference import PianoTranscription

logger = logging.getLogger(__name__)


class ModelManager:
    """
    모델 관리 싱글톤 클래스
    - 모델을 한 번만 로드하고 재사용
    - 멀티스레드 환경에서 안전
    """
    
    _instance = None
    _transcription_model = None
    _lock = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            import threading
            cls._lock = threading.Lock()
        return cls._instance
    
    def __init__(self):
        # 중복 초기화 방지
        if hasattr(self, '_initialized'):
            return
        self._initialized = True
        
        # 환경변수에서 설정 읽기
        self.models_dir = os.getenv('MODELS_DIR', '/app/models')
        self.device = os.getenv('TORCH_DEVICE', 'cpu')
        self.checkpoint_path = os.getenv(
            'PIANO_TRANS_CHECKPOINT',
            os.path.join(self.models_dir, 'piano_transcription/note_F1=0.9677_pedal_F1=0.9186.pth')
        )
        
        logger.info(f"ModelManager initialized: device={self.device}, models_dir={self.models_dir}")
    
    def get_transcription_model(self) -> PianoTranscription:
        """
        피아노 전사 모델 반환 (Lazy Loading + 싱글톤)
        
        Returns:
            PianoTranscription 모델 인스턴스
        """
        if self._transcription_model is None:
            with self._lock:
                # Double-checked locking
                if self._transcription_model is None:
                    logger.info("Loading piano transcription model...")
                    
                    try:
                        # checkpoint가 있으면 사용, 없으면 기본 모델 다운로드
                        if os.path.exists(self.checkpoint_path):
                            logger.info(f"Using local checkpoint: {self.checkpoint_path}")
                            self._transcription_model = PianoTranscription(
                                device=self.device,
                                checkpoint_path=self.checkpoint_path
                            )
                        else:
                            logger.warning(f"Checkpoint not found: {self.checkpoint_path}")
                            logger.info("Downloading default model (this may take a while)...")
                            self._transcription_model = PianoTranscription(
                                device=self.device
                            )
                        
                        logger.info("✓ Piano transcription model loaded successfully")
                        
                    except Exception as e:
                        logger.error(f"Failed to load model: {e}")
                        raise
        
        return self._transcription_model
    
    def unload_models(self):
        """메모리 해제 (테스트/디버깅용)"""
        with self._lock:
            if self._transcription_model is not None:
                del self._transcription_model
                self._transcription_model = None
                logger.info("Models unloaded from memory")


# 글로벌 인스턴스
model_manager = ModelManager()