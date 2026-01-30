from typing import List, Optional
from domain.strategies.post_base import PostProcessStrategy

class IdentityPostStrategy(PostProcessStrategy):
    def __init__(self, target_dim: Optional[int] = None):
        self.target_dim = target_dim

    def apply(self, vec: List[float], current_dim: int):
        # target_dim이 주어져도 일단 그대로(정책상 무변환)
        return vec, current_dim
