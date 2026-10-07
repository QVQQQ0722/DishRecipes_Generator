"""Model access for the graph nodes. Vendor request/response formats stay in this file."""

import os
from dataclasses import dataclass, field
from typing import Literal, Protocol

from pydantic import BaseModel, ValidationError

from app.schemas import Source

Role = Literal["classify", "main"]


class ModelFailure(Exception):
    """A safe, caller-visible failure; never include credentials or raw HTTP bodies."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


@dataclass
class ModelReply:
    parsed: BaseModel | None = None
    text: str = ""
    sources: list[Source] = field(default_factory=list)
    input_tokens: int = 0
    output_tokens: int = 0
    search_requests: int = 0


class ModelClient(Protocol):
    def parse(self, role: Role, system: str, user: str, schema: type[BaseModel]) -> ModelReply:
        """Structured output validated against schema."""
        ...

    def search(self, system: str, user: str) -> ModelReply:
        """Web-grounded notes in text, cited pages in sources."""
        ...


class AzureModelClient:
    """Foundry (Azure OpenAI) Responses API. Not yet run against a real deployment."""

    def __init__(self):
        from openai import OpenAI

        endpoint = os.getenv("AZURE_OPENAI_ENDPOINT", "").strip().rstrip("/")
        key = os.getenv("AZURE_OPENAI_API_KEY", "").strip()
        self.models = {
            "classify": os.getenv("AGENT_MODEL_CLASSIFY", "").strip(),
            "main": os.getenv("AGENT_MODEL_MAIN", "").strip(),
        }
        if not endpoint or not key or not all(self.models.values()):
            raise ModelFailure("MODEL_CONFIG", "Set AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, "
                               "AGENT_MODEL_CLASSIFY and AGENT_MODEL_MAIN in agent/.env.")
        timeout = float(os.getenv("AGENT_MODEL_TIMEOUT_SECONDS", "60"))
        self.client = OpenAI(base_url=f"{endpoint}/openai/v1/", api_key=key, timeout=timeout, max_retries=0)

    def _call(self, method, **kwargs):
        import openai

        try:
            return method(**kwargs)
        except openai.APITimeoutError as exc:
            raise ModelFailure("MODEL_TIMEOUT", "The model request timed out.") from exc
        except openai.OpenAIError as exc:
            raise ModelFailure("MODEL_UNAVAILABLE", f"The model call failed ({type(exc).__name__}).") from exc
        except ValidationError as exc:
            raise ModelFailure("INVALID_MODEL_RESPONSE", "The model output did not match the schema.") from exc

    def parse(self, role: Role, system: str, user: str, schema: type[BaseModel]) -> ModelReply:
        response = self._call(self.client.responses.parse, model=self.models[role],
                              instructions=system, input=user, text_format=schema)
        if response.output_parsed is None:
            raise ModelFailure("INVALID_MODEL_RESPONSE", "The model returned no structured output.")
        return ModelReply(parsed=response.output_parsed, text=response.output_text,
                          input_tokens=response.usage.input_tokens, output_tokens=response.usage.output_tokens)

    def search(self, system: str, user: str) -> ModelReply:
        response = self._call(self.client.responses.create, model=self.models["main"],
                              instructions=system, input=user, tools=[{"type": "web_search"}])
        urls: dict[str, str] = {}
        searches = 0
        for item in response.output:
            if item.type == "web_search_call":
                searches += 1
            elif item.type == "message":
                for part in item.content:
                    for note in getattr(part, "annotations", None) or []:
                        if note.type == "url_citation":
                            urls.setdefault(note.url, note.title or note.url)
        sources = [Source(id=f"s{n}", kind="web", title=title, url=url)
                   for n, (url, title) in enumerate(urls.items(), start=1)]
        return ModelReply(text=response.output_text, sources=sources, search_requests=searches,
                          input_tokens=response.usage.input_tokens, output_tokens=response.usage.output_tokens)
