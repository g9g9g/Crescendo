from domain.strategies.post_base import PostProcessStrategy

class PostprocessStep:
    def __init__(self, strategy: PostProcessStrategy):
        self.strategy = strategy

    def __call__(self, ctx: dict) -> dict:
        vec, dim = self.strategy.apply(ctx["embedding"], ctx["embedding_dim"])
        ctx["embedding"], ctx["embedding_dim"] = vec, dim
        return ctx
