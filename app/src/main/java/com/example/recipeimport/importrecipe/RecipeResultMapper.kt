package com.example.recipeimport.importrecipe

import com.example.recipeimport.Ingredient
import com.example.recipeimport.Recipe
import com.example.recipeimport.RecipeStep

object RecipeResultMapper {
    fun map(result: ExtractedRecipe, input: AnalysisInput): Recipe {
        if (result.ingredients.isEmpty() || result.steps.isEmpty()) {
            throw RecipeImportException(ImportErrorCode.NOT_A_RECIPE, "没有提取到完整菜谱，请补充正文或图片。")
        }
        val evidenceById = input.evidence.associateBy { it.id }
        fun invalid(): Nothing = throw RecipeImportException(ImportErrorCode.INVALID_RESULT, "分析结果格式不完整，请重试。")
        if (result.title.isBlank() || (result.servings != null && result.servings <= 0)) invalid()
        if (evidenceById.size != input.evidence.size || input.evidence.any { it.id.isBlank() }) invalid()
        fun describeEvidence(ids: List<String>): String {
            if (ids.isEmpty() || ids.any { it !in evidenceById }) invalid()
            return ids.joinToString("；") { id ->
                val evidence = evidenceById.getValue(id)
                listOfNotNull(id, evidence.timestampSeconds?.let { "${it}s" }, evidence.text).joinToString("：")
            }
        }
        val ingredients = result.ingredients.map {
            if (it.name.isBlank()) invalid()
            Ingredient(it.name.trim(), it.amount?.trim()?.ifBlank { null }, describeEvidence(it.evidenceIds))
        }
        val steps = result.steps.map {
            if (it.instruction.isBlank()) invalid()
            RecipeStep(it.instruction.trim(), null, describeEvidence(it.evidenceIds))
        }
        val demo = result.isDemo || input.isDemo
        return Recipe(
            title = result.title.trim(), source = input.source, servings = result.servings,
            ingredients = ingredients, steps = steps, isDemo = demo,
            warnings = (result.warnings + if (demo) listOf("演示流程：未读取链接或调用真实 VLM，当前为固定示例菜谱。") else emptyList()).distinct(),
        )
    }
}
