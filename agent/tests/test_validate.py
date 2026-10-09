"""Allergen checks cover ingredient names, step text and tips. Warnings are left free for substitution notes."""

from fakes import DISH, ScriptedModelClient, example_recipe, make_request

from app.graph import generate
from app.graph.validate import allergen_hits
from app.schemas import Ingredient


def with_first_step(text: str, tip: str | None = None):
    recipe = example_recipe()
    recipe.steps[0].instruction, recipe.steps[0].tip = text, tip
    return recipe


def test_hidden_allergen_used_only_in_a_step_is_caught():
    # The Oct 8 eval case: Worcestershire sauce in the dressing step, absent from the ingredient list.
    hits = allergen_hits(with_first_step("Whisk the egg yolk with Worcestershire sauce."), ["fish"])
    assert len(hits) == 1 and hits[0].startswith("step 1 mentions 'worcestershire'")


def test_plural_and_tip_mentions_are_caught():
    assert allergen_hits(with_first_step("Mash two anchovies into the dressing."), ["fish"])
    assert allergen_hits(with_first_step("Beat the eggs.", tip="Top with crushed peanuts."), ["peanut"])


def test_substitution_note_in_ingredient_name_gets_an_actionable_problem():
    # The Oct 8 eval case: a correct swap whose name still says "peanuts".
    recipe = example_recipe()
    recipe.ingredients.append(Ingredient(id="seeds", name="Sunflower seeds (as substitute for peanuts)",
                                         quantity=0.25, unit="cup", origin="estimated", source_ids=[]))
    [hit] = allergen_hits(recipe, ["peanut"])
    assert hit.startswith("ingredient 'Sunflower seeds (as substitute for peanuts)' mentions 'peanuts'")
    assert "explain any substitution only in warnings" in hit


def test_substitution_explained_only_in_warnings_passes():
    recipe = example_recipe()
    recipe.ingredients.append(Ingredient(id="seeds", name="Sunflower seeds", quantity=0.25, unit="cup",
                                         origin="estimated", source_ids=[]))
    recipe.warnings = ["Peanuts were replaced with sunflower seeds because of your peanut allergy."]
    assert allergen_hits(recipe, ["peanut"]) == []


def test_allergen_in_step_text_is_repaired_once():
    draft = with_first_step("Beat the eggs with a dash of fish sauce.")
    llm = ScriptedModelClient(DISH, [draft, example_recipe()])
    response = generate(make_request(allergies=["fish"]), llm)
    assert response.status == "succeeded" and llm.calls.count("Recipe") == 2
