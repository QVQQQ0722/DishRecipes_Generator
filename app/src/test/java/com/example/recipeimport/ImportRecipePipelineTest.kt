package com.example.recipeimport

import com.example.recipeimport.importrecipe.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportRecipePipelineTest {
    private val source = ImportSource("https://www.bilibili.com/video/example", Platform.BILIBILI)
    private val evidence = listOf(AnalysisEvidence("text-1", EvidenceKind.TEXT, "青菜，洗净后炒熟"))
    private val result = ExtractedRecipe("青菜", null,
        listOf(ExtractedIngredient("青菜", null, listOf("text-1"))),
        listOf(ExtractedStep("洗净后炒熟", listOf("text-1"))))

    @Test fun stagesPassMixedContentAndPreserveUnknownQuantities() = runTest {
        val image = PostMedia("image", "https://media.example/image.jpg", "image/jpeg")
        val video = PostMedia("video", "https://media.example/video.mp4", "video/mp4")
        val post = ResolvedPost(source, "青菜", listOf(image), listOf(video))
        val pipeline = ImportRecipePipeline(
            PostExtractor { post },
            MediaPreparer { input ->
                assertEquals(listOf(image), input.images)
                assertEquals(listOf(video), input.videos)
                AnalysisInput(input.source, evidence)
            },
            VlmRecipeAnalyzer { input -> assertEquals(evidence, input.evidence); result },
        )
        val stages = mutableListOf<String>()
        val recipe = pipeline.analyze(source, stages::add)
        assertEquals(ImportStage.entries.map { it.label }, stages)
        assertEquals(source, recipe.source)
        assertNull(recipe.ingredients.first().amount)
        assertNull(recipe.servings)
        assertFalse(recipe.isDemo)
    }

    @Test fun emptyPostFailsBeforeModelIsCalled() = runTest {
        val pipeline = ImportRecipePipeline(
            PostExtractor { ResolvedPost(source, "") },
            MediaPreparer { error("Must not prepare empty content") },
            VlmRecipeAnalyzer { error("Must not call model") },
        )
        try { pipeline.analyze(source) {}; fail("Expected content error") }
        catch (e: RecipeImportException) { assertEquals(ImportErrorCode.CONTENT_UNAVAILABLE, e.code) }
    }

    @Test fun fabricatedEvidenceReferencesAreRejected() {
        val invalid = result.copy(ingredients = listOf(ExtractedIngredient("青菜", "200 g", listOf("missing"))))
        try { RecipeResultMapper.map(invalid, AnalysisInput(source, evidence)); fail("Expected validation error") }
        catch (e: RecipeImportException) { assertEquals(ImportErrorCode.INVALID_RESULT, e.code) }
    }

    @Test fun nonRecipeIsRejected() {
        try {
            RecipeResultMapper.map(result.copy(steps = emptyList()), AnalysisInput(source, evidence))
            fail("Expected non-recipe error")
        } catch (e: RecipeImportException) { assertEquals(ImportErrorCode.NOT_A_RECIPE, e.code) }
    }

    @Test fun demoFlagCannotBeLostByModelResponse() {
        val recipe = RecipeResultMapper.map(result, AnalysisInput(source, evidence, isDemo = true))
        assertTrue(recipe.isDemo)
        assertTrue(recipe.warnings.any { "演示" in it })
    }

    @Test fun cancellationStopsBeforeMediaPreparation() = runTest {
        val entered = CompletableDeferred<Unit>()
        val pipeline = ImportRecipePipeline(
            PostExtractor { entered.complete(Unit); awaitCancellation() },
            MediaPreparer { error("Cancelled job must not continue") },
            VlmRecipeAnalyzer { error("Cancelled job must not call model") },
        )
        val job = launch { pipeline.analyze(source) {} }
        entered.await()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
    }
}
