import os
from pathlib import Path

from dotenv import load_dotenv

from app.errors import ImportFailure
from app.models.demo import DemoVlmRecipeAnalyzer
from app.models.ollama import OllamaVlmRecipeAnalyzer
from app.ports import VlmRecipeAnalyzer


def create_analyzer(provider: str | None = None) -> VlmRecipeAnalyzer:
    load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)
    provider = provider or os.getenv("RECIPE_MODEL_PROVIDER", "demo")
    if provider == "demo":
        return DemoVlmRecipeAnalyzer()
    if provider == "ollama":
        model = os.getenv("RECIPE_MODEL_NAME", "").strip()
        if not model:
            raise ImportFailure("MODEL_CONFIG", "请在 backend/.env 设置 RECIPE_MODEL_NAME。", 503)
        try:
            timeout = float(os.getenv("RECIPE_MODEL_TIMEOUT_SECONDS", "120"))
            if not 1 <= timeout <= 600:
                raise ValueError()
        except ValueError as exc:
            raise ImportFailure("MODEL_CONFIG", "超时时间必须为 1 到 600 秒。", 503) from exc
        return OllamaVlmRecipeAnalyzer(model, os.getenv("RECIPE_MODEL_BASE_URL", "http://127.0.0.1:11434"), timeout)
    raise ImportFailure("MODEL_CONFIG", "未知模型供应商，请在 models/factory.py 注册对应适配器。", 503)
