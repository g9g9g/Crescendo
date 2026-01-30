from typing import Protocol, List

class PostProcessStrategy(Protocol):
    def apply(self, vec: List[float], current_dim: int) -> tuple[List[float], int]:
        ...
