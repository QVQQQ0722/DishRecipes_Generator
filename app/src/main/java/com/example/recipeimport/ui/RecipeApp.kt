package com.example.recipeimport.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.recipeimport.*
import com.example.recipeimport.R

@Composable
fun RecipeApp(state: RecipeUiState, model: RecipeViewModel) {
    RecipeTheme {
        BackHandler(state.screen != RecipeScreen.MENU || state.method != null, model::back)
        Surface(Modifier.fillMaxSize(), color = RecipeColors.Background) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                    // Keep the design's 44dp top region, but let Android draw real system status icons.
                    val safeTop = with(LocalDensity.current) { WindowInsets.safeDrawing.getTop(this).toDp() }
                    Spacer(Modifier.height(maxOf(44.dp, safeTop)))
                    when (state.screen) {
                        RecipeScreen.MENU -> MenuScreen(state, model)
                        RecipeScreen.ADD -> AddMethodsScreen(model::back, model::chooseMethod)
                        RecipeScreen.DETAIL -> state.recipe?.let { DetailScreen(it, model::back) }
                    }
                }
            }
            state.method?.let { CreationDialog(it, state, model) }
        }
    }
}

@Composable
private fun ColumnScope.MenuScreen(state: RecipeUiState, model: RecipeViewModel) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { RecipeHeader(if (state.savedOnly) "收藏的菜谱" else "我的菜谱", "今天想做点什么？") }
        items(state.recipes.filter { !state.savedOnly || it.isSaved }, key = { it.id }) { recipe ->
            Box(Modifier.padding(horizontal = 22.dp).padding(bottom = 16.dp)) {
                RecipeCard(recipe) { model.openRecipe(recipe) }
            }
        }
        item { Box(Modifier.padding(horizontal = 22.dp)) { AddRecipeCard(model::openAdd) } }
    }
    Column(Modifier.fillMaxWidth().background(RecipeColors.Surface)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))) {
        HorizontalDivider(color = RecipeColors.Border, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 46.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(false, true).forEach { saved ->
                val selected = state.savedOnly == saved
                val color = if (selected) RecipeColors.Green else RecipeColors.Muted
                Column(Modifier.widthIn(min = 76.dp).heightIn(min = 52.dp)
                    .selectable(selected, role = Role.Tab, onClick = { model.selectTab(saved) }).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FigmaIcon(if (saved) R.drawable.n0_bookmark else R.drawable.n0_layout_grid, 20.dp, tint = color)
                    RecipeText(if (saved) "Saved" else "Menu", 12, color)
                }
            }
        }
    }
}

@Composable
fun AddMethodsScreen(onBack: () -> Unit, onChoose: (CreationMethod) -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        .verticalScroll(rememberScrollState())) {
        RecipeHeader("增加菜谱", "选择最适合你的创建方式", onBack)
        Column(Modifier.padding(horizontal = 22.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MethodCard("导入菜谱", "从网页链接、照片或文件快速导入", R.drawable.n1_download, RecipeColors.Sage) { onChoose(CreationMethod.IMPORT) }
            MethodCard("自己输入", "逐项添加食材与烹饪步骤", R.drawable.n1_pencil_line, RecipeColors.Peach) { onChoose(CreationMethod.MANUAL) }
            MethodCard("AI 生成", "告诉 AI 现有食材和饮食偏好", R.drawable.n1_sparkles, RecipeColors.Lavender) { onChoose(CreationMethod.AI) }
            Row(Modifier.fillMaxWidth().background(RecipeColors.Sage, RoundedCornerShape(10.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FigmaIcon(R.drawable.n1_lightbulb_off, 18.dp)
                RecipeText("提示：导入后仍可编辑食材、份量和步骤。", 12, RecipeColors.Green,
                    Modifier.weight(1f), lineHeight = 16.8f)
            }
        }
    }
}

@Composable
fun DetailScreen(recipe: Recipe, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
        .verticalScroll(rememberScrollState())) {
        RecipeHeader(recipe.title, listOfNotNull(recipe.minutes?.let { "$it 分钟" },
            recipe.servings?.let { "$it 人份" }, recipe.difficulty).joinToString(" · ").ifBlank { "我的菜谱" }, onBack)
        if (recipe.photoKey != null) {
            Box(Modifier.padding(horizontal = 22.dp)) {
                RecipePhoto(recipe, Modifier.fillMaxWidth().height(166.dp).clip(RoundedCornerShape(24.dp)), detail = true)
            }
        }
        Column(Modifier.padding(horizontal = 22.dp).padding(top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            recipe.warnings.forEach { RecipeText(it, 12, RecipeColors.Muted) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RecipeText("食材 Ingredients", 22)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.ingredients.forEach { IngredientRow(it) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RecipeText("步骤 Steps", 22)
                recipe.steps.forEachIndexed { index, step -> StepItem(index + 1, step) }
            }
        }
    }
}
