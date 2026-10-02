import asyncio
import base64
import json
from unittest.mock import patch

import httpx
import pytest

from app.errors import ImportFailure
from app.extractors.fixture import EXAMPLES, FixturePostExtractor
from app.main import app
from app.media.local import LocalMediaPreparer
from app.models.factory import create_analyzer
from app.models.ollama import OllamaVlmRecipeAnalyzer
from app.pipeline import ImportPipeline, create_pipeline
from app.schemas import AnalysisInput, Evidence, Media, ResolvedPost


def run(coroutine):
    return asyncio.run(coroutine)


def api_request(method, path, **kwargs):
    async def request():
        async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
            return await client.request(method, path, **kwargs)
    return run(request())


def test_demo_result_keeps_missing_amounts_and_trace():
    records = {}
    result = run(create_pipeline("demo").run("fixture://tomato_eggs", records.__setitem__))
    assert result.is_demo and result.is_fixture
    assert result.recipe.title == "番茄炒蛋"
    assert result.recipe.ingredients[2].amount is None
    assert result.recipe.servings is None
    assert list(records) == ["01_post.json", "02_analysis_input.json", "03_model_raw.txt", "04_result.json"]


@pytest.mark.parametrize("raw,code", [
    ("not json", "INVALID_RESULT"),
    ('{"error":"NOT_A_RECIPE"}', "NOT_A_RECIPE"),
    ('{"title":"hello"}', "INVALID_RESULT"),
])
def test_bad_model_results(raw, code):
    class FakeModel:
        provider = "test"
        model = "test"
        is_demo = False

        async def analyze(self, content):
            return raw

    pipeline = ImportPipeline(FixturePostExtractor(), LocalMediaPreparer(EXAMPLES), FakeModel())
    with pytest.raises(ImportFailure) as error:
        run(pipeline.run("fixture://tomato_eggs"))
    assert error.value.code == code


def test_unknown_evidence_rejected():
    pipeline = create_pipeline("demo")
    raw = (EXAMPLES / "demo_response.json").read_text(encoding="utf-8")

    async def bad_response(content):
        return raw.replace("text-1", "invented-id")

    pipeline.analyzer.analyze = bad_response
    with pytest.raises(ImportFailure, match="不存在的证据"):
        run(pipeline.run("fixture://tomato_eggs"))


def test_real_links_not_silently_replaced_with_demo():
    with pytest.raises(ImportFailure) as error:
        run(create_pipeline("demo").run("https://www.bilibili.com/video/example"))
    assert error.value.code == "UNSUPPORTED_SOURCE"


def test_mixed_media_and_timestamps(tmp_path):
    # A minimal PNG fixture; no image library or network required.
    png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a3ioAAAAASUVORK5CYII=")
    (tmp_path / "frame.png").write_bytes(png)
    post = ResolvedPost(source="fixture://test", text="正文", transcript="口述步骤",
        images=[Media(id="image-1", path="frame.png", mime_type="image/png")],
        video_frames=[Media(id="frame-1", path="frame.png", mime_type="image/png", timestamp_seconds=12.0)])
    result = run(LocalMediaPreparer(tmp_path).prepare(post))
    assert [item.kind for item in result.evidence] == ["text", "transcript", "image", "video_frame"]
    assert base64.b64decode(result.evidence[2].image_base64) == png
    assert result.evidence[3].timestamp_seconds == 12


def test_video_fails_explicitly(tmp_path):
    with pytest.raises(ImportFailure) as error:
        run(LocalMediaPreparer(tmp_path).prepare(ResolvedPost(source="fixture://test", videos=["video.mp4"])))
    assert error.value.code == "VIDEO_NOT_IMPLEMENTED"


def test_path_escape_rejected(tmp_path):
    post = ResolvedPost(source="fixture://test", images=[Media(id="image-1", path="../private.png", mime_type="image/png")])
    with pytest.raises(ImportFailure, match="examples"):
        run(LocalMediaPreparer(tmp_path).prepare(post))


def test_api_contract(monkeypatch):
    monkeypatch.setenv("RECIPE_MODEL_PROVIDER", "demo")
    assert api_request("GET", "/health").json() == {"status": "ok"}
    response = api_request("POST", "/v1/imports/example", json={"example_id": "tomato_eggs"})
    assert response.status_code == 200
    assert response.json()["is_demo"] is True
    assert len(response.json()["recipe"]["steps"]) == 3
    assert api_request("POST", "/v1/imports/example", json={"example_id": "../private"}).status_code == 422


def test_invalid_provider_is_error_not_demo(monkeypatch):
    monkeypatch.setenv("RECIPE_MODEL_PROVIDER", "unknown")
    result = api_request("POST", "/v1/imports/example", json={})
    assert result.status_code == 503
    assert result.json()["error"]["code"] == "MODEL_CONFIG"


def test_ollama_requires_model(monkeypatch):
    monkeypatch.setenv("RECIPE_MODEL_NAME", "")
    with pytest.raises(ImportFailure, match="RECIPE_MODEL_NAME"):
        create_analyzer("ollama")


def test_ollama_sends_image_bytes_and_schema():
    def handle(request):
        payload = json.loads(request.content)
        assert request.url.path == "/api/chat"
        assert payload["stream"] is False
        assert payload["messages"][1]["images"] == ["aW1hZ2U="]
        assert "image-1" in payload["messages"][1]["content"]
        assert "Ingredient" in payload["format"]["$defs"]
        return httpx.Response(200, json={"message": {"content": '{"error":"NOT_A_RECIPE"}'}})

    client = httpx.AsyncClient(transport=httpx.MockTransport(handle))
    content = AnalysisInput(source="fixture://test", is_fixture=True,
        evidence=[Evidence(id="image-1", kind="image", image_base64="aW1hZ2U=", mime_type="image/png")])
    with patch("app.models.ollama.httpx.AsyncClient", return_value=client):
        assert run(OllamaVlmRecipeAnalyzer("test", "http://localhost:11434").analyze(content)) == '{"error":"NOT_A_RECIPE"}'


def test_ollama_failure_does_not_fall_back():
    def handle(request):
        return httpx.Response(500, text="private provider diagnostic")

    client = httpx.AsyncClient(transport=httpx.MockTransport(handle))
    content = AnalysisInput(source="fixture://test", is_fixture=True,
        evidence=[Evidence(id="text-1", kind="text", text="番茄炒蛋")])
    with patch("app.models.ollama.httpx.AsyncClient", return_value=client):
        with pytest.raises(ImportFailure) as error:
            run(OllamaVlmRecipeAnalyzer("test", "http://localhost:11434").analyze(content))
    assert error.value.code == "MODEL_UNAVAILABLE"
    assert "private" not in error.value.message
