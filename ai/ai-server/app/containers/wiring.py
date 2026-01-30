from domain.services.embedding_service import EmbeddingService
from domain.strategies.identity import IdentityPostStrategy
from domain.services.omr_service import OMRService
from infra.adapters.s3_uploader import S3Uploader

_service = EmbeddingService(
    post_strategy=IdentityPostStrategy(target_dim=None)  # 그대로(예: 768차원 유지)
    # post_strategy=ProjectToDim(target_dim=1024)        # 필요 시 투영
)

_s3_uploader = S3Uploader()
_omr_service = OMRService(performance_mode="balanced", s3_uploader=_s3_uploader)

def get_embedding_service() -> EmbeddingService:
    return _service



def get_omr_service() -> OMRService:
    return _omr_service
