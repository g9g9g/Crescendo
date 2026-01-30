from typing import Protocol, Dict, Any

class Step(Protocol):
    def __call__(self, ctx: Dict[str, Any]) -> Dict[str, Any]: ...

class Pipeline:
    def __init__(self, *steps: Step):
        self.steps = steps

    def run(self, ctx: Dict[str, Any]) -> Dict[str, Any]:
        for step in self.steps:
            ctx = step(ctx)
        return ctx
