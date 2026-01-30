# app/routes/difficulty.py
"""
MusicXML 난이도 평가 API 엔드포인트
"""

import os
import logging
from flask import Blueprint, request, jsonify
from werkzeug.exceptions import BadRequest
from flasgger import swag_from

from domain.services.difficulty_evaluator import DifficultyEvaluator
from infra.adapters.s3_downloader import S3Downloader

logger = logging.getLogger(__name__)

# Blueprint 생성
difficulty_bp = Blueprint('difficulty', __name__)

# 서비스 인스턴스 (싱글톤)
difficulty_service = None
s3_downloader = None


def get_difficulty_service():
    """난이도 평가 서비스 싱글톤"""
    global difficulty_service
    if difficulty_service is None:
        difficulty_service = DifficultyEvaluator()
    return difficulty_service


def get_s3_downloader():
    """S3 다운로더 싱글톤"""
    global s3_downloader
    if s3_downloader is None:
        s3_downloader = S3Downloader()
    return s3_downloader


@difficulty_bp.route('/evaluation', methods=['POST'])
@swag_from({
    "tags": ["difficulty"],
    "summary": "MusicXML 난이도 평가",
    "description": "S3에 업로드된 MusicXML 파일의 연주 난이도를 평가합니다. 레벨: 브론즈 1~3 (쉬움), 실버 1~3 (보통), 골드 1~3 (어려움). 6가지 지표: 템포, 리듬 복잡도, 음정 도약, 화성 복잡도, 기술적 난이도, 곡 길이",
    "parameters": [
        {
            "in": "body",
            "name": "body",
            "required": True,
            "schema": {
                "type": "object",
                "properties": {
                    "s3_url": {
                        "type": "string",
                        "format": "url",
                        "example": "https://bucket.s3.amazonaws.com/score.musicxml",
                        "description": "S3에 저장된 MusicXML 파일 URL"
                    }
                },
                "required": ["s3_url"]
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
                    "level": {"type": "integer", "example": 5, "description": "난이도 레벨 (1-9)"},
                    "level_name": {"type": "string", "example": "실버 2", "description": "난이도 티어 (브론즈 3->2->1, 실버 3->2->1, 골드 3->2->1 순으로 어려워짐)"},
                    "total_score": {"type": "number", "example": 52.3, "description": "총 점수 (0-100)"},
                    "metrics": {
                        "type": "object",
                        "description": "세부 평가 지표 (각 0-10점)",
                        "properties": {
                            "tempo": {"type": "number", "example": 7.5, "description": "템포"},
                            "rhythm": {"type": "number", "example": 6.8, "description": "리듬 복잡도"},
                            "intervals": {"type": "number", "example": 8.2, "description": "음정 도약"},
                            "harmony": {"type": "number", "example": 5.5, "description": "화성 복잡도"},
                            "technique": {"type": "number", "example": 7.0, "description": "기술적 난이도"},
                            "length": {"type": "number", "example": 6.5, "description": "곡 길이"}
                        }
                    },
                    "summary": {"type": "string", "example": "실버 2 수준의 곡입니다. 특히 리듬 복잡도가 높은 편입니다."},
                    "recommendations": {
                        "type": "array",
                        "items": {"type": "string"},
                        "example": ["손가락 스트레칭 연습 권장", "리듬 연습 집중"]
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
                    "error": {"type": "string", "example": "s3_url is required"}
                }
            }
        },
        "500": {"description": "서버 오류"}
    }
})
def evaluate_difficulty():
    """
    POST /ai/difficulty/evaluation

    MusicXML 난이도 평가 API
    """
    temp_input_path = None

    try:
        # 1. Request 검증
        if not request.is_json:
            raise BadRequest("Content-Type must be application/json")

        data = request.get_json()

        if 's3_url' not in data:
            return jsonify({
                'success': False,
                'error': 's3_url is required'
            }), 400

        s3_url = data['s3_url']

        logger.info(f"Difficulty evaluation: {s3_url}")

        # 2. URL 검증
        downloader = get_s3_downloader()
        if not downloader.is_valid_s3_url(s3_url):
            return jsonify({
                'success': False,
                'error': 'Invalid S3 URL'
            }), 400

        # 3. S3에서 MusicXML 다운로드
        temp_input_path = downloader.download_musicxml(s3_url)

        if not temp_input_path:
            return jsonify({
                'success': False,
                'error': 'Failed to download MusicXML from S3'
            }), 400

        logger.info(f"Downloaded to: {temp_input_path}")

        # 4. 난이도 평가 수행
        evaluator = get_difficulty_service()
        result = evaluator.evaluate(temp_input_path)

        if not result.get('success'):
            return jsonify(result), 400

        logger.info(f"Difficulty evaluation completed: Level {result['level']}")

        # 5. 임시 파일 정리
        if temp_input_path and os.path.exists(temp_input_path):
            try:
                os.remove(temp_input_path)
                logger.info(f"Cleaned up input: {temp_input_path}")
            except Exception as e:
                logger.warning(f"Failed to delete input file: {e}")

        # 6. 결과 반환
        return jsonify({
            'success': True,
            'level': result['level'],
            'level_name': result['level_name'],
            'total_score': result['total_score'],
            'metrics': result['metrics'],
            'summary': result['summary'],
            'recommendations': result['recommendations']
        }), 200

    except BadRequest as e:
        logger.warning(f"Bad request: {e}")
        # 에러 시 임시 파일 정리
        if temp_input_path and os.path.exists(temp_input_path):
            try:
                os.remove(temp_input_path)
            except:
                pass
        return jsonify({
            'success': False,
            'error': str(e)
        }), 400

    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        # 에러 시 임시 파일 정리
        if temp_input_path and os.path.exists(temp_input_path):
            try:
                os.remove(temp_input_path)
            except:
                pass
        return jsonify({
            'success': False,
            'error': 'Internal server error'
        }), 500


@difficulty_bp.route('/health', methods=['GET'])
@swag_from({
    "tags": ["difficulty"],
    "summary": "난이도 평가 서비스 상태 확인",
    "description": "난이도 평가 서비스의 상태를 확인합니다.",
    "responses": {
        "200": {
            "description": "서비스 정상",
            "schema": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "example": "healthy"},
                    "service": {"type": "string", "example": "difficulty_evaluation"}
                }
            }
        }
    }
})
def health_check():
    """
    GET /ai/difficulty/health

    헬스 체크
    """
    try:
        service = get_difficulty_service()

        return jsonify({
            'status': 'healthy',
            'service': 'difficulty_evaluation'
        }), 200

    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return jsonify({
            'status': 'unhealthy',
            'error': str(e)
        }), 500
