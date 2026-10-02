from pathlib import Path

from app.errors import ImportFailure
from app.schemas import ResolvedPost

EXAMPLES = Path(__file__).resolve().parents[2] / "examples"


class FixturePostExtractor:
    """Only reads the allowlisted local example, never downloads a supplied URL."""

    async def extract(self, source: str) -> ResolvedPost:
        if source != "fixture://tomato_eggs":
            raise ImportFailure("UNSUPPORTED_SOURCE", "目前仅支持固定示例 tomato_eggs，尚未接入网站抓取。")
        post = ResolvedPost.model_validate_json(
            (EXAMPLES / "tomato_eggs.json").read_text(encoding="utf-8")
        )
        if post.source != source or not post.is_fixture:
            raise ImportFailure("INVALID_FIXTURE", "固定示例的来源标记不一致。")
        return post
