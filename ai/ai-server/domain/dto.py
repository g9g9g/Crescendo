from pydantic import BaseModel

class EmbeddingResponse(BaseModel):
    embedding_dim: int
    embedding: list[float]

    def model_dump_json(self):
        return {"embedding_dim": self.embedding_dim, "embedding": self.embedding}
