package com.example.recipeimport

enum class Platform(val label: String) {
    XIAOHONGSHU("小红书"), INSTAGRAM("Instagram"), BILIBILI("哔哩哔哩"), TIKTOK("TikTok")
}
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

/** Backward-compatible facade; platform URL handling is owned by the import feature. */
object SourceParser {
    fun parse(text: String): ImportSource =
        com.example.recipeimport.importrecipe.LinkParser.parse(text)
}
