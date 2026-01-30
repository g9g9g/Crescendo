# app/routes/style_transfer.py
"""
MusicXML 스타일 변환 API 엔드포인트
"""

import os
import logging
import tempfile
from flask import Blueprint, request, jsonify
from werkzeug.exceptions import BadRequest
from flasgger import swag_from
from pathlib import Path

from domain.services.style_transfer_service import StyleTransferService
from infra.adapters.s3_downloader import S3Downloader
from infra.adapters.s3_uploader import S3Uploader

logger = logging.getLogger(__name__)

# Blueprint 생성
style_bp = Blueprint('style_transfer', __name__)

# 서비스 인스턴스 (싱글톤)
style_service = None
s3_downloader = None
s3_uploader = None


def get_style_service():
    """스타일 변환 서비스 싱글톤"""
    global style_service
    if style_service is None:
        style_service = StyleTransferService()
    return style_service


def get_s3_downloader():
    """S3 다운로더 싱글톤"""
    global s3_downloader
    if s3_downloader is None:
        s3_downloader = S3Downloader()
    return s3_downloader


def get_s3_uploader():
    """S3 업로더 싱글톤"""
    global s3_uploader
    if s3_uploader is None:
        s3_uploader = S3Uploader()
    return s3_uploader


@style_bp.route('/arrangement', methods=['POST'])
@swag_from({
    "tags": ["style"],
    "summary": "MusicXML 장르 스타일 변환 (규칙 기반)",
    "description": "S3에 업로드된 MusicXML 파일을 규칙 기반 음악 이론을 사용하여 지정한 장르로 변환합니다.",
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
                    },
                    "style": {
                        "type": "string",
                        "enum": ["jazz", "classical", "pop", "bossa_nova", "waltz", "swing", "baroque"],
                        "example": "jazz",
                        "description": "변환할 장르"
                    }
                },
                "required": ["s3_url", "style"]
            }
        }
    ],
    "responses": {
        "200": {
            "description": "변환 성공 - S3 URL 반환",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": True},
                    "s3_url": {"type": "string", "format": "url", "example": "https://bucket.s3.ap-northeast-2.amazonaws.com/musicxml/converted/20251111_converted_jazz.musicxml?..."},
                    "style": {"type": "string", "example": "jazz"},
                    "method": {"type": "string", "example": "rule-based"}
                }
            }
        },
        "400": {
            "description": "요청 오류",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": False},
                    "error": {"type": "string", "example": "s3_url and style are required"}
                }
            }
        },
        "500": {"description": "서버 오류"}
    }
})
def style_transfer():
    """
    POST /ai/style-transfer

    MusicXML 장르 스타일 변환 API
    """
    temp_input_path = None
    temp_output_path = None

    try:
        # 1. Request 검증
        if not request.is_json:
            raise BadRequest("Content-Type must be application/json")

        data = request.get_json()

        if 's3_url' not in data or 'style' not in data:
            return jsonify({
                'success': False,
                'error': 's3_url and style are required'
            }), 400

        s3_url = data['s3_url']
        style = data['style']

        logger.info(f"Style transfer: {s3_url} -> {style} (Rule-based)")

        # 2. 장르 검증
        supported_styles = StyleTransferService.SUPPORTED_STYLES
        if style not in supported_styles:
            return jsonify({
                'success': False,
                'error': f'Unsupported style: {style}',
                'supported_styles': supported_styles
            }), 400

        # 3. URL 검증
        downloader = get_s3_downloader()
        if not downloader.is_valid_s3_url(s3_url):
            return jsonify({
                'success': False,
                'error': 'Invalid S3 URL'
            }), 400

        # 4. S3에서 MusicXML 다운로드
        temp_input_path = downloader.download_musicxml(s3_url)

        if not temp_input_path:
            return jsonify({
                'success': False,
                'error': 'Failed to download MusicXML from S3'
            }), 400

        logger.info(f"Downloaded to: {temp_input_path}")

        # 5. 스타일 변환 수행
        service = get_style_service()

        # 출력 파일 경로
        temp_output_path = tempfile.mktemp(suffix='.musicxml')

        result = service.convert_style(
            input_xml_path=temp_input_path,
            style=style,
            output_xml_path=temp_output_path
        )

        if not result.get('success'):
            return jsonify(result), 400

        logger.info(f"Style transfer completed: {style}")

        # 6. S3에 업로드 (Presigned URL 사용 - 24시간 유효)
        uploader = get_s3_uploader()
        s3_url = uploader.upload_musicxml(
            file_path=temp_output_path,
            use_presigned_url=True,  # Presigned URL 사용 (권장)
            presigned_expiration=86400  # 24시간
        )

        # 7. 임시 파일 정리
        if temp_input_path and os.path.exists(temp_input_path):
            try:
                os.remove(temp_input_path)
                logger.info(f"Cleaned up input: {temp_input_path}")
            except Exception as e:
                logger.warning(f"Failed to delete input file: {e}")

        if temp_output_path and os.path.exists(temp_output_path):
            try:
                os.remove(temp_output_path)
                logger.info(f"Cleaned up output: {temp_output_path}")
            except Exception as e:
                logger.warning(f"Failed to delete output file: {e}")

        # 8. S3 URL 반환
        if not s3_url:
            return jsonify({
                'success': False,
                'error': 'Failed to upload converted file to S3'
            }), 500

        return jsonify({
            'success': True,
            's3_url': s3_url,
            'style': style,
            'method': 'rule-based'
        }), 200

    except BadRequest as e:
        logger.warning(f"Bad request: {e}")
        # 에러 시 임시 파일 정리
        if temp_input_path and os.path.exists(temp_input_path):
            try:
                os.remove(temp_input_path)
            except:
                pass
        if temp_output_path and os.path.exists(temp_output_path):
            try:
                os.remove(temp_output_path)
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
        if temp_output_path and os.path.exists(temp_output_path):
            try:
                os.remove(temp_output_path)
            except:
                pass
        return jsonify({
            'success': False,
            'error': 'Internal server error'
        }), 500


@style_bp.route('/style-transfer/styles', methods=['GET'])
@swag_from({
    "tags": ["style"],
    "summary": "지원 장르 목록 조회",
    "description": "스타일 변환에 사용 가능한 장르 목록을 반환합니다.",
    "responses": {
        "200": {
            "description": "지원 장르 목록",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": True},
                    "styles": {
                        "type": "array",
                        "items": {"type": "string"},
                        "example": ["jazz", "classical", "pop", "bossa_nova", "waltz", "swing", "baroque"]
                    },
                    "descriptions": {
                        "type": "object",
                        "example": {
                            "jazz": "재즈 스타일 (스윙, 블루 노트, 즉흥)",
                            "classical": "클래식 스타일 (전통 화성, 아르페지오)"
                        }
                    }
                }
            }
        }
    }
})
def get_supported_styles():
    """
    GET /ai/style-transfer/styles

    지원 장르 목록 조회
    """
    try:
        descriptions = {
            'jazz': '재즈 스타일 (스윙 리듬, 블루 노트, 즉흥 연주)',
            'classical': '클래식 스타일 (전통 화성, 아르페지오, 레가토)',
            'pop': '팝 스타일 (4/4 비트, 캐치한 멜로디, 백비트)',
            'bossa_nova': '보사노바 스타일 (싱코페이션, 재즈 화성, 라틴 리듬)',
            'waltz': '왈츠 스타일 (3/4 박자, 강-약-약 패턴)',
            'swing': '스윙 스타일 (빅밴드 사운드, 강한 스윙 리듬)',
            'baroque': '바로크 스타일 (대위법, 장식음, 통주저음)'
        }

        return jsonify({
            'success': True,
            'styles': StyleTransferService.SUPPORTED_STYLES,
            'descriptions': descriptions
        }), 200

    except Exception as e:
        logger.error(f"Error getting styles: {e}")
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500


@style_bp.route('/style-transfer/health', methods=['GET'])
@swag_from({
    "tags": ["style"],
    "summary": "스타일 변환 서비스 상태 확인",
    "description": "스타일 변환 서비스의 상태를 확인합니다.",
    "responses": {
        "200": {
            "description": "서비스 정상",
            "schema": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "example": "healthy"},
                    "service": {"type": "string", "example": "style_transfer"},
                    "method": {"type": "string", "example": "rule-based"},
                    "supported_styles": {"type": "array", "items": {"type": "string"}}
                }
            }
        }
    }
})
def health_check():
    """
    GET /ai/style-transfer/health

    헬스 체크
    """
    try:
        service = get_style_service()

        return jsonify({
            'status': 'healthy',
            'service': 'style_transfer',
            'method': 'rule-based',
            'supported_styles': StyleTransferService.SUPPORTED_STYLES
        }), 200

    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return jsonify({
            'status': 'unhealthy',
            'error': str(e)
        }), 500
