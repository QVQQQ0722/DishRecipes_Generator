from typing import Protocol

from app.schemas import AnalysisInput, ResolvedPost


class PostExtractor(Protocol):
    async def extract(self, source: str) -> ResolvedPost: ...


class MediaPreparer(Protocol):
    async def prepare(self, post: ResolvedPost) -> AnalysisInput: ...


class VlmRecipeAnalyzer(Protocol):
    provider: str
    model: str
    is_demo: bool

    async def analyze(self, content: AnalysisInput) -> str:
        """Return raw recipe JSON. The pipeline validates it for every provider."""
        ...
