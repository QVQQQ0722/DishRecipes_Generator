from app.errors import ImportFailure
from app.extractors.fixture import EXAMPLES
from app.schemas import AnalysisInput


class DemoVlmRecipeAnalyzer:
    provider = "demo"
    model = "fixed-tomato-eggs-v1"
    is_demo = True

    async def analyze(self, content: AnalysisInput) -> str:
        if content.source != "fixture://tomato_eggs":
            raise ImportFailure("DEMO_INPUT_UNSUPPORTED", "演示模型只返回固定示例的结果。")
        return (EXAMPLES / "demo_response.json").read_text(encoding="utf-8")
