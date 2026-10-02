"""Optional real HTTP adapter, not enabled or installed by default.

Reference: https://docs.ollama.com/api/chat
"""

from pathlib import Path

import httpx

from app.errors import ImportFailure
from app.schemas import AnalysisInput, ExtractedRecipe


class OllamaVlmRecipeAnalyzer:
    provider = "ollama"
    is_demo = False

    def __init__(self, model: str, base_url: str, timeout: float = 120):
        self.model = model
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    async def analyze(self, content: AnalysisInput) -> str:
        prompt = (Path(__file__).resolve().parents[2] / "prompts" / "recipe.txt").read_text(encoding="utf-8")
        messages = [{"role": "system", "content": prompt}]
        for evidence in content.evidence:
            message = {
                "role": "user",
                "content": f"证据 ID: {evidence.id}; 类型: {evidence.kind}; 时间: {evidence.timestamp_seconds}\n{evidence.text or ''}",
            }
            if evidence.image_base64:
                message["images"] = [evidence.image_base64]
            messages.append(message)
        recipe_schema = ExtractedRecipe.model_json_schema()
        definitions = recipe_schema.pop("$defs", {})
        output_schema = {"$defs": definitions, "oneOf": [recipe_schema, {
            "type": "object", "properties": {"error": {"const": "NOT_A_RECIPE"}},
            "required": ["error"], "additionalProperties": False,
        }]}
        try:
            async with httpx.AsyncClient(timeout=self.timeout, follow_redirects=False) as client:
                response = await client.post(f"{self.base_url}/api/chat", json={
                    "model": self.model, "messages": messages, "stream": False,
                    "format": output_schema,
                    "options": {"temperature": 0},
                })
                response.raise_for_status()
                raw = response.json()["message"]["content"]
                if not isinstance(raw, str):
                    raise ValueError("content must be a string")
                return raw
        except httpx.TimeoutException as exc:
            raise ImportFailure("MODEL_TIMEOUT", "模型请求超时，请检查服务或增大超时时间。", 504) from exc
        except httpx.HTTPError as exc:
            raise ImportFailure("MODEL_UNAVAILABLE", "无法调用模型，请检查服务地址、模型名称和服务运行状态。", 502) from exc
        except (ValueError, KeyError, TypeError) as exc:
            raise ImportFailure("INVALID_MODEL_RESPONSE", "模型服务返回了无法识别的响应。", 502) from exc
