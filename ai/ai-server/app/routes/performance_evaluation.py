# app/routes/performance_evaluation.py
"""
연주 평가 API 엔드포인트
"""

import os
import logging
from flask import Blueprint, request, jsonify
from werkzeug.exceptions import BadRequest
from flasgger import swag_from

from domain.services.evaluation_service import PerformanceEvaluationService
from infra.adapters.s3_downloader import S3Downloader

logger = logging.getLogger(__name__)

# Blueprint 생성
performance_bp = Blueprint('performance', __name__)

# 서비스 인스턴스 (싱글톤)
evaluation_service = None
s3_downloader = None


def get_evaluation_service():
    """평가 서비스 싱글톤"""
    global evaluation_service
    if evaluation_service is None:
        evaluation_service = PerformanceEvaluationService()
    return evaluation_service


def get_s3_downloader():
    """S3 다운로더 싱글톤"""
    global s3_downloader
    if s3_downloader is None:
        s3_downloader = S3Downloader()
    return s3_downloader


@performance_bp.route('/evaluate', methods=['POST'])
@swag_from({
    "tags": ["performance"],
    "summary": "피아노 연주 평가",
    "description": "S3에 업로드된 오디오 파일을 분석하여 연주 품질을 평가합니다. 9가지 메트릭으로 점수를 산출하고 피드백을 제공합니다.",
    "parameters": [
        {
            "in": "body",
            "name": "body",
            "required": True,
            "schema": {
                "type": "object",
                "properties": {
                    "audio_url": {
                        "type": "string",
                        "format": "url",
                        "example": "https://bucket.s3.amazonaws.com/audio.wav",
                        "description": "S3에 저장된 오디오 파일 URL (WAV, MP3, FLAC 등)"
                    }
                },
                "required": ["audio_url"]
            }
        }
    ],
    "responses": {
        "200": {
            "description": "평가 성공",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": True},
                    "overall_score": {"type": "number", "example": 78.5, "description": "종합 점수 (0~100)"},
                    "grade": {"type": "string", "example": "B+", "description": "등급 (S, A+, A, B+, B, C+, C, D)"},
                    "metrics": {
                        "type": "object",
                        "description": "9가지 평가 메트릭",
                        "example": {
                            "tempo_stability": {"name": "템포 안정성", "score": 85.2},
                            "rhythm_consistency": {"name": "리듬 일관성", "score": 72.1}
                        }
                    },
                    "feedback": {
                        "type": "array",
                        "items": {"type": "string"},
                        "example": ["✨ 좋은 연주입니다!", "강점:", "  • 템포 안정성: 85.2점"]
                    },
                    "stats": {
                        "type": "object",
                        "properties": {
                            "total_notes": {"type": "integer", "example": 245},
                            "duration": {"type": "number", "example": 32.5},
                            "avg_velocity": {"type": "number", "example": 68.3}
                        }
                    }
                }
            }
        },
        "400": {
            "description": "요청 오류",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": False},
                    "error": {"type": "string", "example": "audio_url is required"}
                }
            }
        },
        "500": {"description": "서버 오류"}
    }
})
def evaluate_performance():
    """
    POST /ai/evaluate

    피아노 연주 평가 API
    """
    temp_file_path = None
    
    try:
        # 1. Request 검증
        if not request.is_json:
            raise BadRequest("Content-Type must be application/json")
        
        data = request.get_json()
        
        if 'audio_url' not in data:
            return jsonify({
                'success': False,
                'error': 'audio_url is required'
            }), 400
        
        audio_url = data['audio_url']
        logger.info(f"Evaluating audio from: {audio_url}")
        
        # 2. URL 검증
        downloader = get_s3_downloader()
        if not downloader.is_valid_s3_url(audio_url):
            return jsonify({
                'success': False,
                'error': 'Invalid S3 URL'
            }), 400
        
        # 3. S3에서 오디오 다운로드
        temp_file_path = downloader.download_audio(audio_url)
        
        if not temp_file_path:
            return jsonify({
                'success': False,
                'error': 'Failed to download audio from S3'
            }), 400
        
        logger.info(f"Downloaded to: {temp_file_path}")
        
        # 4. 평가 수행
        service = get_evaluation_service()
        result = service.evaluate(
            audio_path=temp_file_path,
            return_midi=False
        )
        
        # 5. 응답
        status_code = 200 if result.get('success') else 400
        
        if result.get('success'):
            logger.info(
                f"Evaluation completed: "
                f"score={result['overall_score']}, "
                f"grade={result['grade']}"
            )
        else:
            logger.warning(f"Evaluation failed: {result.get('error')}")
        
        return jsonify(result), status_code
    
    except BadRequest as e:
        logger.warning(f"Bad request: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 400
    
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        return jsonify({
            'success': False,
            'error': 'Internal server error'
        }), 500
    
    finally:
        # 6. 임시 파일 삭제
        if temp_file_path and os.path.exists(temp_file_path):
            try:
                os.remove(temp_file_path)
                logger.info(f"Cleaned up: {temp_file_path}")
            except Exception as e:
                logger.warning(f"Failed to delete temp file: {e}")


@performance_bp.route('/evaluate/batch', methods=['POST'])
@swag_from({
    "tags": ["performance"],
    "summary": "배치 연주 평가",
    "description": "여러 오디오 파일을 한 번에 평가합니다. 최대 10개까지 처리 가능합니다.",
    "parameters": [
        {
            "in": "body",
            "name": "body",
            "required": True,
            "schema": {
                "type": "object",
                "properties": {
                    "audio_urls": {
                        "type": "array",
                        "items": {
                            "type": "string",
                            "format": "url"
                        },
                        "example": [
                            "https://bucket.s3.amazonaws.com/audio1.wav",
                            "https://bucket.s3.amazonaws.com/audio2.wav"
                        ],
                        "description": "S3 오디오 파일 URL 배열 (최대 10개)"
                    }
                },
                "required": ["audio_urls"]
            }
        }
    ],
    "responses": {
        "200": {
            "description": "배치 평가 완료",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": True},
                    "results": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "audio_url": {"type": "string"},
                                "success": {"type": "boolean"},
                                "overall_score": {"type": "number"},
                                "grade": {"type": "string"}
                            }
                        },
                        "example": [
                            {
                                "audio_url": "https://bucket.s3.amazonaws.com/audio1.wav",
                                "success": True,
                                "overall_score": 85.2,
                                "grade": "A"
                            }
                        ]
                    },
                    "summary": {
                        "type": "object",
                        "properties": {
                            "total": {"type": "integer", "example": 2},
                            "succeeded": {"type": "integer", "example": 2},
                            "failed": {"type": "integer", "example": 0},
                            "avg_score": {"type": "number", "example": 81.85}
                        }
                    }
                }
            }
        },
        "400": {
            "description": "요청 오류",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": False},
                    "error": {"type": "string", "example": "Maximum 10 audio files per batch"}
                }
            }
        },
        "500": {"description": "서버 오류"}
    }
})
def evaluate_performance_batch():
    """
    POST /ai/evaluate/batch

    여러 연주를 한 번에 평가
    """
    try:
        if not request.is_json:
            raise BadRequest("Content-Type must be application/json")
        
        data = request.get_json()
        
        if 'audio_urls' not in data:
            return jsonify({
                'success': False,
                'error': 'audio_urls is required'
            }), 400
        
        audio_urls = data['audio_urls']
        
        if not isinstance(audio_urls, list) or len(audio_urls) == 0:
            return jsonify({
                'success': False,
                'error': 'audio_urls must be a non-empty list'
            }), 400
        
        # 최대 10개로 제한 (과부하 방지)
        if len(audio_urls) > 10:
            return jsonify({
                'success': False,
                'error': 'Maximum 10 audio files per batch'
            }), 400
        
        logger.info(f"Batch evaluation: {len(audio_urls)} files")
        
        # 서비스 초기화
        downloader = get_s3_downloader()
        service = get_evaluation_service()
        
        results = []
        succeeded = 0
        failed = 0
        total_score = 0.0
        
        # 각 파일 평가
        for idx, audio_url in enumerate(audio_urls, 1):
            temp_file_path = None
            
            try:
                logger.info(f"[{idx}/{len(audio_urls)}] Processing: {audio_url}")
                
                # 다운로드
                temp_file_path = downloader.download_audio(audio_url)
                
                if not temp_file_path:
                    results.append({
                        'audio_url': audio_url,
                        'success': False,
                        'error': 'Failed to download'
                    })
                    failed += 1
                    continue
                
                # 평가
                result = service.evaluate(temp_file_path, return_midi=False)
                result['audio_url'] = audio_url
                
                results.append(result)
                
                if result.get('success'):
                    succeeded += 1
                    total_score += result.get('overall_score', 0)
                else:
                    failed += 1
            
            except Exception as e:
                logger.error(f"Error processing {audio_url}: {e}")
                results.append({
                    'audio_url': audio_url,
                    'success': False,
                    'error': str(e)
                })
                failed += 1
            
            finally:
                # 임시 파일 삭제
                if temp_file_path and os.path.exists(temp_file_path):
                    try:
                        os.remove(temp_file_path)
                    except OSError as e:
                        logger.warning(f"Failed to delete temp file {temp_file_path}: {e}")
        
        # 요약
        summary = {
            'total': len(audio_urls),
            'succeeded': succeeded,
            'failed': failed,
            'avg_score': round(total_score / succeeded, 2) if succeeded > 0 else 0.0
        }
        
        logger.info(
            f"Batch completed: "
            f"{succeeded} succeeded, {failed} failed, "
            f"avg_score={summary['avg_score']}"
        )
        
        return jsonify({
            'success': True,
            'results': results,
            'summary': summary
        }), 200
    
    except BadRequest as e:
        logger.warning(f"Bad request: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 400
    
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        return jsonify({
            'success': False,
            'error': 'Internal server error'
        }), 500


@performance_bp.route('/health', methods=['GET'])
@swag_from({
    "tags": ["performance"],
    "summary": "연주 평가 서비스 상태 확인",
    "description": "연주 평가 서비스의 상태와 모델 로드 여부를 확인합니다.",
    "responses": {
        "200": {
            "description": "서비스 정상",
            "schema": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "example": "healthy"},
                    "service": {"type": "string", "example": "performance_evaluation"},
                    "device": {"type": "string", "example": "cpu", "description": "사용 중인 디바이스 (cpu/cuda)"},
                    "model_loaded": {"type": "boolean", "example": True, "description": "모델 로드 완료 여부"},
                    "models_dir": {"type": "string", "example": "/app/models"}
                }
            }
        },
        "500": {
            "description": "서비스 비정상",
            "schema": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "example": "unhealthy"},
                    "error": {"type": "string"}
                }
            }
        }
    }
})
def health_check():
    """
    GET /ai/health

    헬스 체크
    """
    try:
        from infra.model_manager import model_manager
        
        # 모델 로드 여부 확인
        model_loaded = model_manager._transcription_model is not None
        
        return jsonify({
            'status': 'healthy',
            'service': 'performance_evaluation',
            'device': model_manager.device,
            'model_loaded': model_loaded,
            'models_dir': model_manager.models_dir
        }), 200
    
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return jsonify({
            'status': 'unhealthy',
            'error': str(e)
        }), 500


@performance_bp.route('/file-info', methods=['POST'])
@swag_from({
    "tags": ["performance"],
    "summary": "S3 파일 정보 조회",
    "description": "다운로드 전에 S3 파일의 메타데이터를 조회합니다. 파일 존재 여부, 크기, 타입을 확인할 수 있습니다.",
    "parameters": [
        {
            "in": "body",
            "name": "body",
            "required": True,
            "schema": {
                "type": "object",
                "properties": {
                    "audio_url": {
                        "type": "string",
                        "format": "url",
                        "example": "https://bucket.s3.amazonaws.com/audio.wav",
                        "description": "조회할 S3 파일 URL"
                    }
                },
                "required": ["audio_url"]
            }
        }
    ],
    "responses": {
        "200": {
            "description": "파일이 존재함",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": True},
                    "content_type": {"type": "string", "example": "audio/wav"},
                    "content_length": {"type": "integer", "example": 1234567, "description": "파일 크기 (bytes)"},
                    "last_modified": {"type": "string", "example": "Wed, 01 Jan 2025 12:00:00 GMT"},
                    "exists": {"type": "boolean", "example": True}
                }
            }
        },
        "404": {
            "description": "파일이 존재하지 않음",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": False},
                    "exists": {"type": "boolean", "example": False}
                }
            }
        },
        "400": {"description": "요청 오류"},
        "500": {"description": "서버 오류"}
    }
})
def get_file_info():
    """
    POST /ai/file-info

    S3 파일 정보 조회 (다운로드 전 검증용)
    """
    try:
        if not request.is_json:
            raise BadRequest("Content-Type must be application/json")
        
        data = request.get_json()
        
        if 'audio_url' not in data:
            return jsonify({
                'success': False,
                'error': 'audio_url is required'
            }), 400
        
        audio_url = data['audio_url']
        
        downloader = get_s3_downloader()
        file_info = downloader.get_file_info(audio_url)
        
        if file_info.get('exists'):
            return jsonify({
                'success': True,
                **file_info
            }), 200
        else:
            return jsonify({
                'success': False,
                **file_info
            }), 404
    
    except Exception as e:
        logger.error(f"Error getting file info: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500