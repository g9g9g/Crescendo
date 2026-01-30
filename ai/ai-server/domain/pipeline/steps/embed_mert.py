import numpy as np
from util.musicxml_to_mert_embedding_direct import musicxml_to_mert_embedding

class EmbedMertStep:
    def __call__(self, ctx: dict) -> dict:
        path = ctx["local_path"]
        embedding, _features = musicxml_to_mert_embedding(path)
        ctx["embedding"] = embedding.astype(float).tolist()
        ctx["embedding_dim"] = int(np.asarray(embedding).shape[0])
        return ctx
