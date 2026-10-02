package com.example.recipeimport.importrecipe

import com.example.recipeimport.ImportSource
import com.example.recipeimport.Recipe
import com.example.recipeimport.RecipeRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** One callable entry point, with replaceable stages; no Activity/Compose dependency. */
class ImportRecipePipeline(
    private val extractor: PostExtractor,
    private val mediaPreparer: MediaPreparer,
    private val analyzer: VlmRecipeAnalyzer,
    private val demoMode: Boolean = false,
) : RecipeRepository {
    override suspend fun analyze(source: ImportSource, onProgress: (String) -> Unit): Recipe {
        suspend fun progress(stage: ImportStage) {
            currentCoroutineContext().ensureActive()
            onProgress((if (demoMode) "演示：" else "") + stage.label)
        }
        progress(ImportStage.FETCH_CONTENT)
        val post = extractor.extract(source)
        if (post.source != source) {
            throw RecipeImportException(ImportErrorCode.INVALID_RESULT, "帖子来源与导入链接不一致。")
        }
        if (post.text.isBlank() && post.images.isEmpty() && post.videos.isEmpty()) {
            throw RecipeImportException(ImportErrorCode.CONTENT_UNAVAILABLE, "无法读取帖子内容，请补充正文或媒体。")
        }
        progress(ImportStage.PREPARE_MEDIA)
        val prepared = mediaPreparer.prepare(post)
        val input = prepared.copy(isDemo = demoMode || post.isDemo || prepared.isDemo)
        if (input.source != source || input.evidence.isEmpty()) {
            throw RecipeImportException(ImportErrorCode.CONTENT_UNAVAILABLE, "没有可供模型分析的内容。")
        }
        progress(ImportStage.ANALYZE_CONTENT)
        val result = analyzer.analyze(input)
        progress(ImportStage.VALIDATE_RESULT)
        return RecipeResultMapper.map(result, input)
    }
}
