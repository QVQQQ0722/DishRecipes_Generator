package com.example.recipeimport.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.recipeimport.R

object RecipeColors {
    val Background = Color(0xFFF6F4EE)
    val Surface = Color(0xFFFFFEFA)
    val Ink = Color(0xFF243127)
    val Muted = Color(0xFF758077)
    val Green = Color(0xFF2F6B4F)
    val Border = Color(0xFFE3E5DE)
    val Sage = Color(0xFFDDEADF)
    val Peach = Color(0xFFF6E7D8)
    val Lavender = Color(0xFFE7E3F4)
}

private val Inter = FontFamily(Font(R.font.inter))

@Composable
fun RecipeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = RecipeColors.Green, onPrimary = Color.White,
        background = RecipeColors.Background, onBackground = RecipeColors.Ink,
        surface = RecipeColors.Surface, onSurface = RecipeColors.Ink,
        outline = RecipeColors.Border,
    ), content = content)
}

@Composable
fun RecipeText(
    text: String,
    size: Int = 14,
    color: Color = RecipeColors.Ink,
    modifier: Modifier = Modifier,
    lineHeight: Float = size * 1.21f,
) {
    Text(text, modifier = modifier, color = color, style = TextStyle(
        fontFamily = Inter, fontSize = size.sp, lineHeight = lineHeight.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    ))
}
