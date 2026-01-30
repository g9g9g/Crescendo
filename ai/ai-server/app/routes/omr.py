# app/routes/omr.py
from flask import Blueprint, request, jsonify, send_file
from flasgger import swag_from
from app.containers.wiring import get_omr_service

bp = Blueprint("omr", __name__)


@bp.route('/health', methods=['GET'])
@swag_from({
    "tags": ["omr"],
    "summary": "OMR 서비스 헬스체크",
    "description": "OMR 서비스가 정상적으로 실행되고 있는지 확인합니다.",
    "responses": {
        "200": {
            "description": "서비스 정상",
            "schema": {
                "type": "object",
                "properties": {
                    "status": {"type": "string", "example": "ok"},
                    "message": {"type": "string", "example": "OMR Backend is running"}
                }
            }
        }
    }
})
def health_check():
    """Health check endpoint"""
    return jsonify({'status': 'ok', 'message': 'OMR Backend is running'})


@bp.route('/performance', methods=['GET'])
@swag_from({
    "tags": ["omr"],
    "summary": "성능 설정 조회",
    "description": "현재 OMR 엔진의 성능 설정과 시스템 정보를 조회합니다.",
    "responses": {
        "200": {
            "description": "성능 정보",
            "schema": {
                "type": "object",
                "properties": {
                    "performance_mode": {"type": "string", "example": "balanced"},
                    "config": {
                        "type": "object",
                        "properties": {
                            "dpi": {"type": "integer"},
                            "grayscale": {"type": "boolean"},
                            "skip_deskew": {"type": "boolean"},
                            "cache_enabled": {"type": "boolean"},
                            "max_workers": {"type": "integer"}
                        }
                    },
                    "system": {
                        "type": "object",
                        "properties": {
                            "cpu_count": {"type": "integer"},
                            "gpu_available": {"type": "boolean"},
                            "gpu_enabled": {"type": "boolean"}
                        }
                    },
                    "omr_engines": {
                        "type": "object",
                        "properties": {
                            "audiveris_available": {"type": "boolean"},
                            "oemer_available": {"type": "boolean"},
                            "active_engine": {"type": "string"}
                        }
                    }
                }
            }
        }
    }
})
def get_performance_info():
    """Get current performance configuration"""
    svc = get_omr_service()
    return jsonify(svc.get_performance_info())


@bp.route('/performance/mode/<mode>', methods=['POST'])
@swag_from({
    "tags": ["omr"],
    "summary": "성능 모드 변경",
    "description": "OMR 엔진의 성능 모드를 변경합니다. fast(빠름), balanced(균형), accurate(정확함) 중 선택할 수 있습니다.",
    "parameters": [
        {
            "in": "path",
            "name": "mode",
            "type": "string",
            "required": True,
            "description": "성능 모드",
            "enum": ["fast", "balanced", "accurate"]
        }
    ],
    "responses": {
        "200": {
            "description": "모드 변경 성공",
            "schema": {
                "type": "object",
                "properties": {
                    "message": {"type": "string", "example": "Performance mode changed to balanced"},
                    "config": {
                        "type": "object",
                        "properties": {
                            "dpi": {"type": "integer"},
                            "grayscale": {"type": "boolean"},
                            "skip_deskew": {"type": "boolean"},
                            "cache_enabled": {"type": "boolean"},
                            "max_workers": {"type": "integer"}
                        }
                    }
                }
            }
        },
        "400": {"description": "잘못된 모드 (fast, balanced, accurate 중 하나여야 함)"}
    }
})
def set_performance_mode(mode):
    """Change performance mode (fast/balanced/accurate)"""
    svc = get_omr_service()
    try:
        result = svc.set_performance_mode(mode)
        return jsonify(result)
    except ValueError as e:
        return jsonify({'error': str(e)}), 400


@bp.route('/convert', methods=['POST'])
@swag_from({
    "tags": ["omr"],
    "summary": "PDF/이미지를 MusicXML로 변환 (S3 업로드)",
    "description": "PDF 또는 이미지 파일을 업로드하여 MusicXML 악보로 변환하고 S3에 업로드합니다. 여러 페이지는 자동으로 하나의 MusicXML로 합쳐집니다. Audiveris 또는 OEMER 엔진을 사용합니다.",
    "consumes": ["multipart/form-data"],
    "parameters": [
        {
            "in": "formData",
            "name": "file",
            "type": "file",
            "required": True,
            "description": "PDF, PNG, JPG 파일 (최대 크기: 10MB)"
        }
    ],
    "responses": {
        "200": {
            "description": "변환 및 S3 업로드 성공",
            "schema": {
                "type": "object",
                "properties": {
                    "success": {"type": "boolean", "example": True},
                    "s3_url": {"type": "string", "example": "https://bucket.s3.ap-northeast-2.amazonaws.com/musicxml/converted/20231112_123456_score.musicxml", "description": "S3에 업로드된 MusicXML URL (Presigned URL, 24시간 유효)"},
                    "musicxml": {"type": "string", "nullable": True, "description": "S3 업로드 실패 시에만 반환되는 MusicXML 내용"},
                    "pages_processed": {"type": "integer", "example": 3},
                    "message": {"type": "string", "example": "Successfully converted 3 page(s) and uploaded to S3"}
                }
            }
        },
        "400": {"description": "잘못된 요청 (파일 없음, 잘못된 파일 형식)"},
        "500": {"description": "변환 실패"}
    }
})
def convert_to_musicxml():
    """Convert PDF/Image to MusicXML"""
    svc = get_omr_service()

    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400

    file = request.files['file']

    if file.filename == '':
        return jsonify({'error': 'No file selected'}), 400

    if not svc.allowed_file(file.filename):
        return jsonify({'error': 'Invalid file type. Only PDF, PNG, JPG allowed'}), 400

    try:
        result = svc.convert_to_musicxml(file)
        return jsonify(result)
    except Exception as e:
        return jsonify({'error': f'Conversion failed: {str(e)}'}), 500


@bp.route('/download/<filename>', methods=['GET'])
@swag_from({
    "tags": ["omr"],
    "summary": "변환된 MusicXML 파일 다운로드",
    "description": "변환된 MusicXML 파일을 다운로드합니다.",
    "parameters": [
        {
            "in": "path",
            "name": "filename",
            "type": "string",
            "required": True,
            "description": "다운로드할 파일명"
        }
    ],
    "produces": ["application/vnd.recordare.musicxml+xml", "application/json"],
    "responses": {
        "200": {
            "description": "파일 다운로드",
            "headers": {
                "Content-Type": {"type": "string", "description": "application/vnd.recordare.musicxml+xml"}
            }
        },
        "404": {"description": "파일을 찾을 수 없음"}
    }
})
def download_file(filename):
    """Download converted MusicXML file"""
    import os
    svc = get_omr_service()
    file_path = os.path.join(svc.output_folder, filename)
    if os.path.exists(file_path):
        return send_file(file_path, as_attachment=True)
    return jsonify({'error': 'File not found'}), 404
