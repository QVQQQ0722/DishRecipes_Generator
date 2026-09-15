package com.example.recipeimport

/** Stable IDs and asset keys keep API/domain objects independent of Android resources. */
object DemoRecipes {
    val recipes = listOf(
        Recipe(
            id = "pasta", title = "番茄罗勒意面", source = null, servings = 2,
            photoKey = "pasta", minutes = 25, difficulty = "简单", cardNote = "简单", isSaved = true,
            ingredients = listOf(
                Ingredient("意大利面", "200 g", "Figma 示例", IngredientIcon.WHEAT),
                Ingredient("樱桃番茄", "250 g", "Figma 示例"),
                Ingredient("新鲜罗勒", "1 把", "Figma 示例", IngredientIcon.LEAF),
                Ingredient("帕玛森芝士", "40 g", "Figma 示例"),
            ),
            steps = listOf(
                "煮一锅盐水，放入意大利面，按包装时间煮至弹牙。",
                "平底锅加橄榄油，放入番茄翻炒至微微裂开。",
                "加入沥干的意面和少量面汤，翻拌至酱汁包裹均匀。",
                "关火后拌入罗勒与帕玛森芝士，即可装盘。",
            ).map { RecipeStep(it, null, "Figma 示例") }, warnings = emptyList(),
        ),
        Recipe(
            id = "salmon", title = "味噌烤三文鱼", source = null, servings = 4,
            photoKey = "salmon", minutes = 35, cardNote = "4 人份",
            ingredients = listOf(
                Ingredient("三文鱼", "4 块", "模拟数据"),
                Ingredient("味噌", "2 汤匙", "模拟数据"),
                Ingredient("西兰花", "1 颗", "模拟数据", IngredientIcon.LEAF),
            ),
            steps = listOf("将味噌抹在三文鱼表面，腌制入味。", "三文鱼和西兰花放入烤盘，烤至鱼肉熟透。", "装盘，搭配喜欢的主食。")
                .map { RecipeStep(it, null, "模拟数据") }, warnings = emptyList(),
        ),
        Recipe(
            id = "chicken", title = "柠檬香草鸡", source = null, servings = 4,
            photoKey = "chicken", minutes = 45, cardNote = "高蛋白",
            ingredients = listOf(
                Ingredient("鸡肉", "600 g", "模拟数据"),
                Ingredient("柠檬", "1 个", "模拟数据"),
                Ingredient("迷迭香", "2 枝", "模拟数据", IngredientIcon.LEAF),
            ),
            steps = listOf("柠檬切片，与香草一起腌制鸡肉。", "将鸡肉放入烤盘，烤至完全熟透。", "稍作静置，切块后装盘。")
                .map { RecipeStep(it, null, "模拟数据") }, warnings = emptyList(),
        ),
    )
}

object ManualRecipeParser {
    fun create(title: String, ingredients: String, steps: String): Recipe {
        require(title.isNotBlank()) { "请填写菜谱名称。" }
        val parsedIngredients = ingredients.lineSequence().filter { it.isNotBlank() }.map { line ->
            val parts = line.split('|', limit = 2).map(String::trim)
            require(parts.first().isNotBlank()) { "食材名称不能为空。" }
            Ingredient(parts.first(), parts.getOrNull(1)?.ifBlank { null }, "用户输入")
        }.toList()
        val parsedSteps = steps.lineSequence().map(String::trim).filter(String::isNotBlank)
            .map { RecipeStep(it, null, "用户输入") }.toList()
        require(parsedIngredients.isNotEmpty()) { "请至少填写一种食材。" }
        require(parsedSteps.isNotEmpty()) { "请至少填写一个步骤。" }
        return Recipe(title.trim(), null, null, parsedIngredients, parsedSteps, emptyList(), isDemo = false)
    }
}
