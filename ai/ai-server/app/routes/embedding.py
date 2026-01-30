# app/routes/embedding.py
from flask import Blueprint, request, jsonify
from pydantic import BaseModel, HttpUrl, ValidationError
from app.containers.wiring import get_embedding_service
from flasgger import swag_from

bp = Blueprint("embedding", __name__)

class EmbeddingRequest(BaseModel):
    s3_url: HttpUrl

@bp.get("/health")
def health():
    return jsonify({"status": "ok"})

@bp.post("/embedding")
@swag_from({
    "tags": ["embedding"],
    "summary": "MusicXML 임베딩 생성",
    "description": "S3 URL로 MusicXML/MIDI/WAV를 지정하면 임베딩 벡터를 반환합니다.",
    "parameters": [
        {
            "in": "body",
            "name": "body",
            "required": True,
            "schema": {
                "type": "object",
                "properties": {
                    "s3_url": {"type": "string", "format": "url", "example": "https://bucket.s3.amazonaws.com/file.musicxml"}
                },
                "required": ["s3_url"]
            }
        }
    ],
    "responses": {
        "200": {
            "description": "성공",
            "schema": {
                "type": "object",
                "properties": {
                    "embedding_dim": {"type": "integer", "example": 768},
                    "embedding": {
                        "type": "array",
                        "items": {"type": "number"},
                        "example": [0.0123, -0.211, 0.993]
                    }
                }
            }
        },
        "400": {"description": "요청 형식 오류"},
        "500": {"description": "서버 오류"}
    }
})
def create_embedding():
    try:
        data = EmbeddingRequest(**request.get_json(force=True))
    except ValidationError as e:
        return jsonify({"error": "invalid_request", "details": e.errors()}), 400

    service = get_embedding_service()
    result = service.run(s3_url=str(data.s3_url))
    return jsonify(result), 200
