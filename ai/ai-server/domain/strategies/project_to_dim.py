from typing import List
import numpy as np
from domain.strategies.post_base import PostProcessStrategy

class ProjectToDim(PostProcessStrategy):
    def __init__(self, target_dim: int, seed: int = 13):
        self.target_dim = target_dim
        self.seed = seed
        self._proj = None

    def _ensure(self, src_dim: int):
        if self._proj is None or self._proj.shape != (self.target_dim, src_dim):
            rng = np.random.default_rng(self.seed)
            P = rng.normal(0, 1.0 / np.sqrt(src_dim), size=(self.target_dim, src_dim))
            self._proj = P

    def apply(self, vec: List[float], current_dim: int):
        self._ensure(current_dim)
        out = np.asarray(self._proj @ np.asarray(vec, dtype=np.float32))
        # L2 정규화(옵션)
        n = np.linalg.norm(out)
        if n > 1e-8:
            out = out / n
        return out.astype(float).tolist(), int(out.shape[0])
