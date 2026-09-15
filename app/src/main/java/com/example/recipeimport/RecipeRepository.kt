package com.example.recipeimport

import kotlinx.coroutines.delay

/** Production implementation should call a backend; never embed model API secrets in the APK. */
interface RecipeRepository {
    suspend fun analyze(source: ImportSource, onProgress: (String) -> Unit): Recipe
}

class DemoRecipeRepository : RecipeRepository {
    override suspend fun analyze(source: ImportSource, onProgress: (String) -> Unit): Recipe {
        listOf("演示：识别分享链接", "演示：模拟文字 / 图片 / 视频分析", "演示：整理食材和步骤").forEach {
            onProgress(it)
            delay(650)
        }
        return Recipe(
            title = "番茄炒蛋（示例菜谱）",
            source = source,
            servings = 2,
            ingredients = listOf(
                Ingredient("番茄", "2 个", "演示数据"),
                Ingredient("鸡蛋", "3 个", "演示数据"),
                Ingredient("食用油", null, "演示数据，分量待确认"),
                Ingredient("盐", null, "演示数据，分量待确认"),
            ),
            steps = listOf(
                RecipeStep("番茄切块，鸡蛋打散。", null, "演示数据"),
                RecipeStep("锅中加油，倒入蛋液炒至凝固，盛出备用。", null, "演示数据"),
                RecipeStep("炒软番茄，加入鸡蛋和盐，翻炒均匀。", null, "演示数据"),
            ),
            warnings = listOf("这份菜谱是固定演示数据，并非从链接提取。", "真实分析中，原帖未提供的用量、时间和温度需标记待确认。"),
        )
    }
}
