package com.example.recipeimport.importrecipe

import com.example.recipeimport.ImportSource

/** Transport-neutral types. Production media processing and model credentials live on the backend. */
data class PostMedia(val id: String, val location: String, val mimeType: String)
data class ResolvedPost(
    val source: ImportSource,
    val text: String,
    val images: List<PostMedia> = emptyList(),
    val videos: List<PostMedia> = emptyList(),
    val isDemo: Boolean = false,
)

enum class EvidenceKind { TEXT, IMAGE, VIDEO_FRAME, TRANSCRIPT }
data class AnalysisEvidence(
    val id: String,
    val kind: EvidenceKind,
    val text: String? = null,
    val media: PostMedia? = null,
    val timestampSeconds: Double? = null,
)
data class AnalysisInput(
    val source: ImportSource,
    val evidence: List<AnalysisEvidence>,
    val isDemo: Boolean = false,
)

data class ExtractedIngredient(val name: String, val amount: String?, val evidenceIds: List<String>)
data class ExtractedStep(val instruction: String, val evidenceIds: List<String>)
data class ExtractedRecipe(
    val title: String,
    val servings: Int?,
    val ingredients: List<ExtractedIngredient>,
    val steps: List<ExtractedStep>,
    val warnings: List<String> = emptyList(),
    val isDemo: Boolean = false,
)

enum class ImportStage(val label: String) {
    FETCH_CONTENT("获取帖子正文、图片和视频"),
    PREPARE_MEDIA("准备图片、视频关键帧和音频转写"),
    ANALYZE_CONTENT("VLM 分析内容并提取菜谱"),
    VALIDATE_RESULT("校验菜名、食材、步骤和证据"),
}

enum class ImportErrorCode { CONTENT_UNAVAILABLE, NOT_A_RECIPE, INVALID_RESULT }
class RecipeImportException(val code: ImportErrorCode, message: String) : Exception(message)
