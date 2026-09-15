package com.example.recipeimport.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.recipeimport.*
import com.example.recipeimport.R

@Composable
fun FigmaIcon(@DrawableRes resource: Int, size: Dp, description: String? = null, tint: Color? = null) {
    Image(painterResource(resource), description, Modifier.size(size),
        colorFilter = tint?.let { ColorFilter.tint(it) })
}

@Composable
fun RecipeHeader(title: String, subtitle: String, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onBack != null) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(RecipeColors.Surface)
                .border(1.dp, RecipeColors.Border, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "返回主菜单", onClick = onBack), Alignment.Center) {
                FigmaIcon(R.drawable.n1_arrow_left, 19.dp, "返回主菜单")
            }
        }
        RecipeText(title, 30, lineHeight = 34.5f)
        RecipeText(subtitle, 14, RecipeColors.Muted, lineHeight = 20.3f)
    }
}

private fun photoResource(key: String?, detail: Boolean): Int? = when (key) {
    "pasta" -> if (detail) R.drawable.n2_recipe_photo else R.drawable.n0_photo
    "salmon" -> R.drawable.n0_photo1
    "chicken" -> R.drawable.n0_photo2
    else -> null
}

@Composable
fun RecipePhoto(recipe: Recipe, modifier: Modifier, detail: Boolean = false) {
    val resource = photoResource(recipe.photoKey, detail)
    if (resource != null) Image(painterResource(resource), recipe.title, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(RecipeColors.Sage), Alignment.Center) {
        FigmaIcon(R.drawable.n2_leaf, 32.dp, "未添加菜谱照片", RecipeColors.Green)
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().shadow(8.dp, shape, ambientColor = RecipeColors.Ink.copy(alpha = .07f),
        spotColor = RecipeColors.Ink.copy(alpha = .07f)).clip(shape).background(RecipeColors.Surface)
        .clickable(role = Role.Button, onClick = onClick).heightIn(min = 112.dp).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RecipePhoto(recipe, Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeText(recipe.title, 16)
            RecipeText(listOfNotNull(recipe.minutes?.let { "$it 分钟" }, recipe.cardNote)
                .joinToString(" · ").ifBlank { "我的菜谱" }, 12, RecipeColors.Muted)
        }
        FigmaIcon(R.drawable.n0_chevron_right, 18.dp)
    }
}

@Composable
fun AddRecipeCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(RecipeColors.Sage).drawBehind {
        drawRoundRect(RecipeColors.Green, Offset(.5.dp.toPx(), .5.dp.toPx()),
            Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()), CornerRadius(16.dp.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))))
    }.clickable(role = Role.Button, onClick = onClick).heightIn(min = 82.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(44.dp).background(RecipeColors.Green, CircleShape), Alignment.Center) {
            FigmaIcon(R.drawable.n0_plus, 22.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RecipeText("增加菜谱", 16, RecipeColors.Green)
            RecipeText("导入、手动输入或让 AI 创作", 12, RecipeColors.Muted)
        }
    }
}

@Composable
fun MethodCard(title: String, subtitle: String, @DrawableRes icon: Int, background: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(RecipeColors.Surface).border(1.dp, RecipeColors.Border, shape)
        .clickable(role = Role.Button, onClick = onClick).heightIn(min = 132.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(58.dp).background(background, RoundedCornerShape(16.dp)), Alignment.Center) {
            FigmaIcon(icon, 27.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeText(title, 22)
            RecipeText(subtitle, 14, RecipeColors.Muted, lineHeight = 20.3f)
        }
        FigmaIcon(R.drawable.n1_chevron_right, 18.dp)
    }
}

@Composable
fun IngredientRow(ingredient: Ingredient) {
    Row(Modifier.fillMaxWidth().background(RecipeColors.Surface, RoundedCornerShape(10.dp))
        .heightIn(min = 54.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).background(RecipeColors.Peach, CircleShape), Alignment.Center) {
            FigmaIcon(when (ingredient.icon) {
                IngredientIcon.WHEAT -> R.drawable.n2_wheat
                IngredientIcon.LEAF -> R.drawable.n2_leaf
                IngredientIcon.CIRCLE -> R.drawable.n2_circle_x
            }, 18.dp)
        }
        RecipeText(ingredient.name, modifier = Modifier.weight(1f))
        RecipeText(ingredient.amount ?: "待确认", color = RecipeColors.Green,
            modifier = Modifier.widthIn(max = 120.dp))
    }
}

@Composable
fun StepItem(number: Int, step: RecipeStep) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).background(RecipeColors.Green, CircleShape), Alignment.Center) {
            RecipeText(number.toString(), color = Color.White)
        }
        RecipeText(step.instruction, lineHeight = 21.7f, modifier = Modifier.weight(1f))
    }
}
