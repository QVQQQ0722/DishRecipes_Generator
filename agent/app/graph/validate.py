"""Code checks after generation: unit sanity, citations, references, allergen match."""

import re

from langchain_core.runnables import RunnableConfig

from app.graph.state import State
from app.schemas import ErrorInfo, GenerateRequest, Recipe, Source

MAX_SYNTHESIZE_ATTEMPTS = 2  # first draft + one repair retry

# Synonyms and common derivatives; matched as whole words against ingredient names.
ALLERGEN_TERMS = {
    "peanut": ["peanut", "groundnut", "satay"],
    "tree nut": ["almond", "walnut", "cashew", "pecan", "pistachio", "hazelnut", "macadamia", "pine nut"],
    "milk": ["milk", "butter", "cheese", "cream", "yogurt", "ghee", "whey"],
    "egg": ["egg", "mayonnaise", "meringue"],
    "soy": ["soy", "soybean", "tofu", "edamame", "miso", "tempeh"],
    "wheat": ["wheat", "flour", "breadcrumb", "panko", "seitan", "noodle", "pasta"],
    "fish": ["fish", "anchovy", "salmon", "tuna", "cod", "bonito"],
    "shellfish": ["shrimp", "prawn", "crab", "lobster", "clam", "mussel", "oyster", "scallop"],
    "sesame": ["sesame", "tahini"],
}


def allergen_hits(recipe: Recipe, allergies: list[str]) -> list[str]:
    hits = []
    for allergy in allergies:
        key = allergy.strip().lower()
        terms = ALLERGEN_TERMS.get(key, []) + [key]
        pattern = re.compile(r"\b(" + "|".join(re.escape(term) for term in terms) + r")(e?s)?\b")
        hits += [f"ingredient '{item.name}' conflicts with allergy '{allergy}'"
                 for item in recipe.ingredients if pattern.search(item.name.lower())]
    return hits


def check_recipe(recipe: Recipe, request: GenerateRequest, sources: list[Source]) -> list[str]:
    problems = []
    if recipe.servings != request.servings:
        problems.append(f"servings must be {request.servings}, got {recipe.servings}")
    if not recipe.ingredients or not recipe.steps:
        problems.append("recipe needs at least one ingredient and one step")
    if min(recipe.times.prep_min, recipe.times.cook_min) < 0 or recipe.times.total_min <= 0:
        problems.append("times must be positive minutes")
    ids = [item.id for item in recipe.ingredients]
    if len(ids) != len(set(ids)):
        problems.append("ingredient ids must be unique")
    known_sources = {source.id for source in sources}
    for item in recipe.ingredients:
        if (item.quantity is None) != (item.unit == "to_taste") or (item.quantity is not None and item.quantity <= 0):
            problems.append(f"ingredient '{item.id}' needs a positive quantity, or null with unit to_taste")
        if not set(item.source_ids) <= known_sources:
            problems.append(f"ingredient '{item.id}' cites an unknown source id")
        if item.origin == "extracted" and not item.source_ids:
            problems.append(f"ingredient '{item.id}' is marked extracted but cites no source")
    if [step.order for step in recipe.steps] != list(range(1, len(recipe.steps) + 1)):
        problems.append("steps must be ordered 1..n")
    for step in recipe.steps:
        if any(used.ingredient_id not in ids for used in step.ingredients_used):
            problems.append(f"step {step.order} references an unknown ingredient id")
    return problems + allergen_hits(recipe, request.profile.allergies)


def validate(state: State, config: RunnableConfig):
    problems = check_recipe(state["recipe"], state["request"], state["sources"])
    update: dict = {"problems": problems}
    if problems and state["attempts"] >= MAX_SYNTHESIZE_ATTEMPTS:
        update["error"] = ErrorInfo(code="VALIDATION_FAILED", message="; ".join(problems))
    return update
