package com.example.recipeimport.importrecipe

import com.example.recipeimport.ImportSource

/** Future real implementation selects an authorized platform adapter on the backend. */
fun interface PostExtractor {
    suspend fun extract(source: ImportSource): ResolvedPost
}

/** Future real implementation performs image loading, keyframe extraction and transcription. */
fun interface MediaPreparer {
    suspend fun prepare(post: ResolvedPost): AnalysisInput
}

/** Future real implementation submits actual content to the selected VLM, not just the page URL. */
fun interface VlmRecipeAnalyzer {
    suspend fun analyze(input: AnalysisInput): ExtractedRecipe
}
