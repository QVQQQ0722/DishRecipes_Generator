package com.example.recipeimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun prepare() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun eachCardOpensItsOwnRecipeAndReturnsHome() {
        val model = RecipeViewModel()
        DemoRecipes.recipes.forEach {
            model.openRecipe(it)
            assertEquals(it.id, model.state.value.recipe?.id)
            assertEquals(RecipeScreen.DETAIL, model.state.value.screen)
            model.back()
            assertEquals(RecipeScreen.MENU, model.state.value.screen)
        }
    }

    @Test fun allCreationMethodsOpenAndDismissToMethodsPage() {
        val model = RecipeViewModel()
        model.openAdd()
        CreationMethod.entries.forEach {
            model.chooseMethod(it)
            assertEquals(it, model.state.value.method)
            model.back()
            assertNull(model.state.value.method)
            assertEquals(RecipeScreen.ADD, model.state.value.screen)
        }
        model.back()
        assertEquals(RecipeScreen.MENU, model.state.value.screen)
    }

    @Test fun manualRecipePreservesUnknownAmountsAndStepOrder() {
        val model = RecipeViewModel()
        model.saveManual("清炒青菜", "青菜 | 200 g\n盐", "洗净青菜\n炒熟后调味")
        val recipe = model.state.value.recipe!!
        assertEquals("200 g", recipe.ingredients.first().amount)
        assertNull(recipe.ingredients.last().amount)
        assertEquals(listOf("洗净青菜", "炒熟后调味"), recipe.steps.map { it.instruction })
        assertEquals(recipe.id, model.state.value.recipes.first().id)
        assertFalse(recipe.isDemo)
    }

    @Test fun invalidManualRecipeDoesNotAddOrNavigate() {
        val model = RecipeViewModel()
        model.openAdd()
        model.chooseMethod(CreationMethod.MANUAL)
        model.saveManual("", "盐", "翻炒")
        assertNotNull(model.state.value.error)
        assertEquals(3, model.state.value.recipes.size)
        assertEquals(RecipeScreen.ADD, model.state.value.screen)
    }

    @Test fun cancelledImportCannotNavigateLater() = runTest(dispatcher) {
        val model = RecipeViewModel()
        model.receiveShare("https://xhslink.com/a/example")
        model.analyze()
        model.closeMethod()
        advanceUntilIdle()
        assertEquals(3, model.state.value.recipes.size)
        assertEquals(RecipeScreen.ADD, model.state.value.screen)
        assertFalse(model.state.value.loading)
    }

    @Test fun importFinishesWithSourceAndDemoWarning() = runTest(dispatcher) {
        val model = RecipeViewModel()
        model.receiveShare("https://www.instagram.com/reel/demo/")
        model.analyze()
        advanceUntilIdle()
        assertEquals(RecipeScreen.DETAIL, model.state.value.screen)
        assertEquals(Platform.INSTAGRAM, model.state.value.recipe?.source?.platform)
        assertTrue(model.state.value.recipe!!.warnings.isNotEmpty())
    }

    @Test fun aiEntryValidatesInputAndMarksSimulation() = runTest(dispatcher) {
        val model = RecipeViewModel()
        model.generateDemo("")
        assertNotNull(model.state.value.error)
        model.generateDemo("番茄和意面")
        advanceUntilIdle()
        assertEquals(RecipeScreen.DETAIL, model.state.value.screen)
        assertTrue(model.state.value.recipe!!.isDemo)
        assertTrue(model.state.value.recipe!!.warnings.first().contains("固定示例"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun ingredientWithoutNameIsRejected() { ManualRecipeParser.create("菜谱", "| 200 g", "煮熟") }
}
