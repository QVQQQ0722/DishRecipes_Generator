"""The eval scorer checks must_not_have in step text and tips too, but not in warnings."""

from fakes import example_recipe

from app.schemas import GenerateResponse
from evals.run_eval import score

CASE = {"dish": "Caesar salad", "profile": {"allergies": ["fish"]}, "must_have": [["egg"]],
        "must_not_have": ["worcestershire"]}


def response_with(recipe) -> GenerateResponse:
    return GenerateResponse(run_id="run_test", status="succeeded", recipe=recipe)


def test_unlisted_ingredient_in_a_step_fails_the_case():
    recipe = example_recipe()
    recipe.steps[0].instruction = "Whisk the egg yolk with Worcestershire sauce."
    assert score(CASE, response_with(recipe)) == ["unwanted in steps: worcestershire"]


def test_mention_in_warnings_only_passes():
    recipe = example_recipe()
    recipe.warnings = ["Worcestershire sauce was left out because it contains anchovies."]
    assert score(CASE, response_with(recipe)) == []
