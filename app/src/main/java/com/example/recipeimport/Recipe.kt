package com.example.recipeimport

import java.net.URI

enum class Platform(val label: String) { XIAOHONGSHU("小红书"), INSTAGRAM("Instagram") }
data class ImportSource(val url: String, val platform: Platform)
enum class IngredientIcon { WHEAT, CIRCLE, LEAF }
data class Ingredient(
    val name: String,
    val amount: String?,
    val evidence: String,
    val icon: IngredientIcon = IngredientIcon.CIRCLE,
)
data class RecipeStep(val instruction: String, val duration: String?, val evidence: String)
data class Recipe(
    val title: String,
    val source: ImportSource?,
    val servings: Int?,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>,
    val warnings: List<String>,
    val isDemo: Boolean = true,
    val id: String = java.util.UUID.randomUUID().toString(),
    val photoKey: String? = null,
    val minutes: Int? = null,
    val difficulty: String? = null,
    val cardNote: String? = null,
    val isSaved: Boolean = false,
)

object SourceParser {
    fun parse(text: String): ImportSource {
        val urls = Regex("https?://[^\\s<>\"，。！？、）】]+", RegexOption.IGNORE_CASE)
            .findAll(text).map { it.value.trimEnd('.', ',', ')', ']', ';') }
        for (url in urls) {
            val uri = runCatching { URI(url) }.getOrNull() ?: continue
            val host = uri.host?.lowercase() ?: continue
            if (uri.userInfo != null || uri.port !in listOf(-1, 80, 443)) continue
            fun belongsTo(domain: String) = host == domain || host.endsWith(".$domain")
            val platform = when {
                belongsTo("xiaohongshu.com") || host == "xhslink.com" -> Platform.XIAOHONGSHU
                belongsTo("instagram.com") -> Platform.INSTAGRAM
                else -> continue
            }
            return ImportSource(url, platform)
        }
        throw IllegalArgumentException("请粘贴包含小红书或 Instagram 链接的分享文字。")
    }
}
