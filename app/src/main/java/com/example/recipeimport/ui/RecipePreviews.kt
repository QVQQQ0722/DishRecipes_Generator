package com.example.recipeimport.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.recipeimport.DemoRecipes
import com.example.recipeimport.RecipeScreen
import com.example.recipeimport.RecipeUiState
import com.example.recipeimport.RecipeViewModel

// Open this file in Android Studio and select Split or Design. No phone/API key required.
@Preview(name = "主菜单", widthDp = 390, heightDp = 844, showBackground = true, apiLevel = 35)
@Composable
fun MenuPreview() {
    RecipeApp(RecipeUiState(), remember { RecipeViewModel() })
}

@Preview(name = "添加方式", widthDp = 390, heightDp = 844, showBackground = true, apiLevel = 35)
@Composable
fun AddMethodsPreview() {
    RecipeApp(RecipeUiState(screen = RecipeScreen.ADD), remember { RecipeViewModel() })
}

@Preview(name = "菜谱详情", widthDp = 390, heightDp = 914, showBackground = true, apiLevel = 35)
@Composable
fun DetailPreview() {
    RecipeApp(RecipeUiState(screen = RecipeScreen.DETAIL, recipe = DemoRecipes.recipes.first()),
        remember { RecipeViewModel() })
}
