# util/audio_loader.py
"""
오디오 파일 로딩 유틸리티
"""

import os
import logging
import librosa
import numpy as np
from typing import Tuple, Optional

logger = logging.getLogger(__name__)


def load_audio_file(
    audio_path: str,
    sample_rate: int = 16000,
    mono: bool = True,
    duration: Optional[float] = None,
    offset: float = 0.0
) -> Tuple[np.ndarray, int]:
    """
    오디오 파일 로드

    Args:
        audio_path: 오디오 파일 경로
        sample_rate: 목표 샘플레이트 (Hz)
        mono: 모노로 변환 여부
        duration: 로드할 길이 (초), None이면 전체
        offset: 시작 오프셋 (초)

    Returns:
        (audio_array, sample_rate) 튜플
        - audio_array: float32 numpy array, shape (n_samples,)
        - sample_rate: 샘플레이트 (Hz)

    Raises:
        FileNotFoundError: 파일이 없는 경우
        ValueError: 오디오 로드 실패
    """
    # 파일 존재 확인
    if not os.path.exists(audio_path):
        raise FileNotFoundError(f"Audio file not found: {audio_path}")

    try:
        logger.info(f"Loading audio: {audio_path} (sr={sample_rate})")

        # librosa로 오디오 로드
        audio, sr = librosa.load(
            audio_path,
            sr=sample_rate,
            mono=mono,
            duration=duration,
            offset=offset
        )

        # 정규화 (옵션)
        if np.max(np.abs(audio)) > 0:
            audio = audio / np.max(np.abs(audio))

        logger.info(
            f"Audio loaded: shape={audio.shape}, "
            f"duration={len(audio)/sr:.2f}s, "
            f"sr={sr}Hz"
        )

        return audio, sr

    except Exception as e:
        logger.error(f"Failed to load audio: {e}")
        raise ValueError(f"Failed to load audio file: {e}")


def get_audio_info(audio_path: str) -> dict:
    """
    오디오 파일 정보 조회 (로드하지 않고 메타데이터만)

    Args:
        audio_path: 오디오 파일 경로

    Returns:
        {
            'duration': float,  # 길이 (초)
            'sample_rate': int,  # 샘플레이트
            'channels': int,     # 채널 수
        }
    """
    try:
        duration = librosa.get_duration(path=audio_path)

        # 실제 샘플레이트는 로드해야 알 수 있음
        y, sr = librosa.load(audio_path, sr=None, duration=0.1)

        return {
            'duration': duration,
            'sample_rate': sr,
            'channels': 1 if y.ndim == 1 else y.shape[0]
        }

    except Exception as e:
        logger.error(f"Failed to get audio info: {e}")
        return {
            'duration': 0.0,
            'sample_rate': 0,
            'channels': 0
        }


def validate_audio_format(audio_path: str) -> bool:
    """
    오디오 파일 형식 검증

    Args:
        audio_path: 오디오 파일 경로

    Returns:
        유효 여부
    """
    valid_extensions = ['.wav', '.mp3', '.flac', '.ogg', '.m4a', '.aac']
    _, ext = os.path.splitext(audio_path)

    if ext.lower() not in valid_extensions:
        logger.warning(f"Unsupported audio format: {ext}")
        return False

    return True
