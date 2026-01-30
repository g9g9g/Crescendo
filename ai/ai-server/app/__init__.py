# app/__init__.py
from flask import Flask
from flask_cors import CORS
from dotenv import load_dotenv
from app.routes.embedding import bp as embedding_bp
from app.routes.omr import bp as omr_bp
from app.routes.performance_evaluation import performance_bp
from app.routes.style_transfer import style_bp
from app.routes.difficulty import difficulty_bp
from flasgger import Swagger

# .env 파일 로드 (프로젝트 루트에서)
load_dotenv()

def create_app():
    app = Flask(__name__)

    # CORS 설정 추가
    CORS(app, origins=["*"])

    # HTTPS 설정
    app.config['PREFERRED_URL_SCHEME'] = 'https'

    # JSON 한글 인코딩 설정
    app.config['JSON_AS_ASCII'] = False
    
    # 모든 blueprint를 /ai 아래로 이동
    app.register_blueprint(embedding_bp, url_prefix="/ai")
    app.register_blueprint(omr_bp, url_prefix="/ai/omr")
    app.register_blueprint(performance_bp, url_prefix="/ai")
    app.register_blueprint(style_bp, url_prefix="/ai")
    app.register_blueprint(difficulty_bp, url_prefix="/ai/difficulty")

    # Swagger 기본 정보
    template = {
        "swagger": "2.0",
        "info": {
            "title": "AI Media API",
            "description": "MusicXML 임베딩, MP3→MusicXML 변환, PDF/Image→MusicXML(OMR), 연주 평가, 장르 스타일 변환, 난이도 평가 API",
            "version": "1.0.0"
        },
        "basePath": "/",  # 중복 방지를 위해 "/" 로 설정
        "schemes": ["https"],  # HTTPS만 사용
        "consumes": ["application/json", "multipart/form-data"],
        "produces": ["application/json", "application/vnd.recordare.musicxml+xml"],
        "tags": [
            {"name": "embedding", "description": "MusicXML 임베딩"},
            {"name": "audio", "description": "오디오 변환(MP3→MusicXML)"},
            {"name": "omr", "description": "악보 인식(PDF/Image→MusicXML)"},
            {"name": "performance", "description": "연주 평가"},
            {"name": "style", "description": "장르 스타일 변환"},
            {"name": "difficulty", "description": "난이도 평가"}
        ],
    }
    Swagger(app, template=template, config={
        "headers": [],
        "specs": [
            {
                "endpoint": "openapi",
                "route": "/ai/openapi.json",
            }
        ],
        "static_url_path": "/ai/flasgger_static",
        "swagger_ui": True,
        "specs_route": "/ai/docs/",
    })
    return app