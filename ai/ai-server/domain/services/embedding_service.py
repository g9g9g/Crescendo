from domain.pipeline.base import Pipeline
from domain.pipeline.steps.download_s3 import DownloadS3Step
from domain.pipeline.steps.embed_mert import EmbedMertStep
from domain.pipeline.steps.postprocess import PostprocessStep
from domain.strategies.post_base import PostProcessStrategy
from infra.adapters.s3_downloader import S3Downloader
from domain.dto import EmbeddingResponse
import os

class EmbeddingService:
    def __init__(self, post_strategy: PostProcessStrategy):
        self.downloader = S3Downloader()
        self.post_strategy = post_strategy

    def run(self, s3_url: str) -> dict:
        pipe = Pipeline(
            DownloadS3Step(self.downloader),
            EmbedMertStep(),
            PostprocessStep(self.post_strategy),
        )
        ctx = pipe.run({"s3_url": s3_url})
        # 임시파일 정리
        try:
            if "local_path" in ctx and os.path.exists(ctx["local_path"]):
                os.remove(ctx["local_path"])
        except: 
            pass
        resp = EmbeddingResponse(embedding_dim=ctx["embedding_dim"], embedding=ctx["embedding"])
        return resp.model_dump_json()
