package com.example.recipeimport

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun menuDetailAndMethodsMatchNavigation() {
        compose.onNodeWithText("我的菜谱").assertIsDisplayed()
        screenshot("figma-home")
        compose.onNodeWithText("番茄罗勒意面").performClick()
        compose.onNodeWithText("食材 Ingredients").assertIsDisplayed()
        compose.onNodeWithText("200 g").assertIsDisplayed()
        screenshot("figma-detail")
        compose.onNodeWithText("关火后拌入罗勒与帕玛森芝士，即可装盘。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("返回主菜单").performScrollTo().performClick()
        compose.onNodeWithText("增加菜谱").performScrollTo().performClick()
        compose.onNodeWithText("自己输入").assertIsDisplayed()
        compose.onNodeWithText("AI 生成").assertIsDisplayed()
        screenshot("figma-add")
        compose.onNodeWithContentDescription("返回主菜单").performClick()
        compose.onNodeWithText("我的菜谱").assertIsDisplayed()
    }

    @Test fun manualEntryCreatesRecipeAndShowsItsSteps() {
        compose.onNodeWithText("增加菜谱").performScrollTo().performClick()
        compose.onNodeWithText("自己输入").performClick()
        compose.onNodeWithText("菜谱名称").performTextInput("测试青菜")
        compose.onNodeWithText("食材与数量").performTextInput("青菜 | 200 g\n盐")
        compose.onNodeWithText("做法（每行一步）").performScrollTo().performTextInput("洗净青菜\n炒熟后调味")
        compose.onNodeWithText("添加菜谱").performScrollTo().performClick()
        compose.onNodeWithText("测试青菜").assertIsDisplayed()
        compose.onNodeWithText("待确认").assertIsDisplayed()
        compose.onNodeWithText("炒熟后调味").performScrollTo().assertIsDisplayed()
    }
}
