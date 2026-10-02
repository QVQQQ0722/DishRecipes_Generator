import json
from collections.abc import Callable

from pydantic import ValidationError

from app.errors import ImportFailure
from app.extractors.fixture import EXAMPLES, FixturePostExtractor
from app.media.local import LocalMediaPreparer
from app.models.factory import create_analyzer
from app.ports import MediaPreparer, PostExtractor, VlmRecipeAnalyzer
from app.schemas import ExtractedRecipe, ImportResult


class ImportPipeline:
    def __init__(self, extractor: PostExtractor, preparer: MediaPreparer, analyzer: VlmRecipeAnalyzer):
        self.extractor = extractor
        self.preparer = preparer
        self.analyzer = analyzer

    async def run(self, source: str, trace: Callable[[str, str], None] | None = None) -> ImportResult:
        def record(name: str, value: str):
            if trace:
                trace(name, value)

        post = await self.extractor.extract(source)
        record("01_post.json", post.model_dump_json(indent=2))
        content = await self.preparer.prepare(post)
        if post.source != source or content.source != source:
            raise ImportFailure("INVALID_SOURCE", "处理过程中来源发生变化。")
        record("02_analysis_input.json", content.model_dump_json(indent=2))
        raw = await self.analyzer.analyze(content)
        record("03_model_raw.txt", raw)
        try:
            value = json.loads(raw)
            if isinstance(value, dict) and value.get("error") == "NOT_A_RECIPE":
                raise ImportFailure("NOT_A_RECIPE", "输入内容不足以整理成菜谱。")
            recipe = ExtractedRecipe.model_validate(value)
        except (ValueError, ValidationError) as exc:
            raise ImportFailure("INVALID_RESULT", "模型输出不符合菜谱结构，请查看原始输出。", 502) from exc
        known = {item.id for item in content.evidence}
        for item in [*recipe.ingredients, *recipe.steps]:
            if not set(item.evidence_ids).issubset(known):
                raise ImportFailure("INVALID_EVIDENCE", "模型引用了不存在的证据。", 502)
        for ingredient in recipe.ingredients:
            if ingredient.amount == "":
                ingredient.amount = None
        if self.analyzer.is_demo:
            recipe.warnings.append("固定演示输出，未调用真实 VLM；修改输入不会让演示模型重新理解内容。")
        result = ImportResult(
            source=source, provider=self.analyzer.provider, model=self.analyzer.model,
            is_demo=self.analyzer.is_demo, is_fixture=post.is_fixture or content.is_fixture,
            stages=["fetch_content", "prepare_media", "analyze_content", "validate_result"], recipe=recipe,
        )
        record("04_result.json", result.model_dump_json(indent=2))
        return result


def create_pipeline(provider: str | None = None) -> ImportPipeline:
    return ImportPipeline(FixturePostExtractor(), LocalMediaPreparer(EXAMPLES), create_analyzer(provider))
