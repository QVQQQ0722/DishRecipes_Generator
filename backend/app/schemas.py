"""Shared input/output contracts. JSON uses snake_case throughout the Python API."""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


class Contract(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True, strict=True)


class Media(Contract):
    id: str = Field(min_length=1)
    path: str = Field(min_length=1)
    mime_type: Literal["image/png", "image/jpeg", "image/webp"]
    timestamp_seconds: float | None = Field(default=None, ge=0)


class ResolvedPost(Contract):
    source: str = Field(min_length=1)
    text: str = ""
    images: list[Media] = Field(default_factory=list)
    videos: list[str] = Field(default_factory=list)
    video_frames: list[Media] = Field(default_factory=list)
    transcript: str = ""
    is_fixture: bool = True


class Evidence(Contract):
    id: str = Field(min_length=1)
    kind: Literal["text", "image", "video_frame", "transcript"]
    text: str | None = None
    image_base64: str | None = None
    mime_type: str | None = None
    timestamp_seconds: float | None = None


class AnalysisInput(Contract):
    source: str
    evidence: list[Evidence] = Field(min_length=1)
    is_fixture: bool

    @model_validator(mode="after")
    def unique_evidence(self):
        ids = [item.id for item in self.evidence]
        if len(ids) != len(set(ids)):
            raise ValueError("证据 ID 不能重复")
        return self


class Ingredient(Contract):
    name: str = Field(min_length=1)
    amount: str | None = Field(description="原材料未提供数量时必须为 null")
    evidence_ids: list[str] = Field(min_length=1)


class Step(Contract):
    instruction: str = Field(min_length=1)
    evidence_ids: list[str] = Field(min_length=1)


class ExtractedRecipe(Contract):
    title: str = Field(min_length=1)
    servings: int | None = Field(ge=1)
    ingredients: list[Ingredient] = Field(min_length=1)
    steps: list[Step] = Field(min_length=1)
    warnings: list[str]


class ImportResult(Contract):
    source: str
    provider: str
    model: str
    is_demo: bool
    is_fixture: bool
    stages: list[str]
    recipe: ExtractedRecipe


class ExampleRequest(Contract):
    example_id: Literal["tomato_eggs"] = "tomato_eggs"
