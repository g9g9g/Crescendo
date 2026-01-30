# infra/adapters/s3_downloader.py
"""
S3 파일 다운로더 (MusicXML, 오디오 파일 등)
"""

import os
import tempfile
import requests
import logging
import re
from typing import Tuple, Optional
from urllib.parse import urlparse

logger = logging.getLogger(__name__)


class S3Downloader:
    """
    S3 URL에서 파일 다운로드
    - MusicXML 파일
    - 오디오 파일 (WAV, MP3, FLAC, etc.)
    """
    
    def __init__(self, timeout: int = 60):
        """
        Args:
            timeout: 다운로드 타임아웃 (초)
        """
        self.timeout = timeout

    def convert_to_global_url(self, s3_url: str) -> str:
        """
        지역 S3 URL을 글로벌 URL로 변환

        Args:
            s3_url: S3 URL (지역 또는 글로벌)

        Returns:
            글로벌 형식의 S3 URL

        Examples:
            https://bucket.s3.ap-northeast-2.amazonaws.com/key
            -> https://bucket.s3.amazonaws.com/key

            https://bucket.s3.us-east-1.amazonaws.com/key
            -> https://bucket.s3.amazonaws.com/key
        """
        # 지역별 S3 엔드포인트를 글로벌로 변환
        # 패턴: .s3.{region}.amazonaws.com -> .s3.amazonaws.com
        pattern = r'\.s3\.[a-z0-9-]+\.amazonaws\.com'
        replacement = '.s3.amazonaws.com'

        converted_url = re.sub(pattern, replacement, s3_url)

        if converted_url != s3_url:
            logger.info(f"Converted regional URL to global: {s3_url} -> {converted_url}")

        return converted_url
    
    def download_to_temp(self, url: str, prefix: str = "file_") -> Tuple[str, str]:
        """
        S3 URL에서 파일을 다운로드하여 임시 파일로 저장 (기존 메서드)
        
        Args:
            url: S3 URL
            prefix: 임시 파일 prefix
        
        Returns:
            (temp_file_path, original_filename) 튜플
        
        Raises:
            Exception: 다운로드 실패 시
        """
        
        # 지역 URL을 글로벌 URL로 변환
        url = self.convert_to_global_url(url)
        # 파일명 추정
        name = os.path.basename(url.split("?")[0]) or "input.file"
        suffix = os.path.splitext(name)[1] or ".dat"
        
        # 임시 파일 생성
        fd, tmp = tempfile.mkstemp(prefix=prefix, suffix=suffix)
        
        try:
            logger.info(f"Downloading from S3: {url}")
            
            with requests.get(url, stream=True, timeout=self.timeout) as r:
                r.raise_for_status()
                
                # 파일 크기 확인 (헤더에서)
                content_length = r.headers.get('Content-Length')
                if content_length:
                    logger.info(f"File size: {int(content_length):,} bytes")
                
                # 청크 단위로 다운로드
                with os.fdopen(fd, "wb") as f:
                    for chunk in r.iter_content(chunk_size=8192):
                        if chunk:
                            f.write(chunk)
            
            # 다운로드 완료 후 파일 크기 확인
            file_size = os.path.getsize(tmp)
            logger.info(f"Downloaded successfully: {tmp} ({file_size:,} bytes)")
            
            # 최소 크기 검증
            if file_size < 100:  # 100 bytes
                logger.warning(f"File too small: {file_size} bytes")
                raise ValueError(f"Downloaded file is too small: {file_size} bytes")
            
            return tmp, name
        
        except Exception as e:
            # 오류 발생 시 임시 파일 정리
            logger.error(f"Download failed: {e}")
            try:
                os.close(fd)
            except:
                pass
            try:
                os.remove(tmp)
            except:
                pass
            raise
    
    def download_audio(self, url: str) -> Optional[str]:
        """
        오디오 파일 다운로드 (연주 평가용)
        
        Args:
            url: S3 오디오 파일 URL
        
        Returns:
            임시 파일 경로, 실패 시 None
        """
        try:
            temp_path, _ = self.download_to_temp(url, prefix="audio_")
            
            # 오디오 파일 확장자 검증
            valid_extensions = ['.wav', '.mp3', '.flac', '.ogg', '.m4a']
            _, ext = os.path.splitext(temp_path)
            
            if ext.lower() not in valid_extensions:
                logger.warning(f"Unexpected audio format: {ext}")
                # 그래도 반환 (librosa가 처리할 수 있을 수도 있음)
            
            return temp_path
        
        except Exception as e:
            logger.error(f"Failed to download audio: {e}")
            return None
    
    def download_musicxml(self, url: str) -> Optional[str]:
        """
        MusicXML 파일 다운로드
        
        Args:
            url: S3 MusicXML 파일 URL
        
        Returns:
            임시 파일 경로, 실패 시 None
        """
        try:
            temp_path, _ = self.download_to_temp(url, prefix="xml_")
            return temp_path
        
        except Exception as e:
            logger.error(f"Failed to download MusicXML: {e}")
            return None
    
    def download_with_retry(
        self, 
        url: str, 
        max_retries: int = 3,
        prefix: str = "file_"
    ) -> Tuple[Optional[str], Optional[str]]:
        """
        재시도 로직이 포함된 다운로드
        
        Args:
            url: S3 URL
            max_retries: 최대 재시도 횟수
            prefix: 임시 파일 prefix
        
        Returns:
            (temp_file_path, original_filename) 튜플, 실패 시 (None, None)
        """
        for attempt in range(1, max_retries + 1):
            try:
                logger.info(f"Download attempt {attempt}/{max_retries}")
                return self.download_to_temp(url, prefix=prefix)
            
            except requests.exceptions.Timeout:
                logger.warning(f"Timeout on attempt {attempt}")
                if attempt == max_retries:
                    logger.error("Max retries reached")
                    return None, None
            
            except Exception as e:
                logger.error(f"Error on attempt {attempt}: {e}")
                if attempt == max_retries:
                    return None, None
        
        return None, None
    
    def is_valid_s3_url(self, url: str) -> bool:
        """
        S3 URL 유효성 검증
        
        Args:
            url: 검증할 URL
        
        Returns:
            유효 여부
        """
        try:
            parsed = urlparse(url)
            
            # 스킴 확인
            if parsed.scheme not in ['http', 'https']:
                return False
            
            # 호스트 확인
            hostname = parsed.hostname
            if not hostname:
                return False
            
            # S3 도메인 패턴
            s3_patterns = [
                's3.amazonaws.com',
                '.s3.amazonaws.com',
                '.s3.',  # 리전별 S3 (예: s3.ap-northeast-2.amazonaws.com)
            ]
            
            return any(pattern in hostname for pattern in s3_patterns)
        
        except Exception:
            return False
    
    def get_file_info(self, url: str) -> dict:
        """
        파일 정보 조회 (HEAD 요청)
        
        Args:
            url: S3 URL
        
        Returns:
            {
                'content_type': str,
                'content_length': int,
                'last_modified': str,
                'exists': bool
            }
        """
        try:
            response = requests.head(url, timeout=10)
            response.raise_for_status()
            
            return {
                'content_type': response.headers.get('Content-Type', 'unknown'),
                'content_length': int(response.headers.get('Content-Length', 0)),
                'last_modified': response.headers.get('Last-Modified', 'unknown'),
                'exists': True
            }
        
        except Exception as e:
            logger.error(f"Failed to get file info: {e}")
            return {
                'content_type': 'unknown',
                'content_length': 0,
                'last_modified': 'unknown',
                'exists': False,
                'error': str(e)
            }