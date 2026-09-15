package com.example.recipeimport.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.recipeimport.*

/** Extra forms are functional prototype flows; the three base screens follow Figma. */
@Composable
fun CreationDialog(method: CreationMethod, state: RecipeUiState, model: RecipeViewModel) {
    var title by rememberSaveable(method) { mutableStateOf("") }
    var ingredients by rememberSaveable(method) { mutableStateOf("") }
    var steps by rememberSaveable(method) { mutableStateOf("") }
    var prompt by rememberSaveable(method) { mutableStateOf("") }
    Dialog(onDismissRequest = model::closeMethod,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(20.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center) {
            Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight),
                shape = RoundedCornerShape(24.dp), color = RecipeColors.Surface) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RecipeText(when (method) {
                        CreationMethod.IMPORT -> "导入菜谱"
                        CreationMethod.MANUAL -> "自己输入"
                        CreationMethod.AI -> "AI 生成"
                    }, 22)
                    when (method) {
                        CreationMethod.IMPORT -> {
                            RecipeText("粘贴小红书或 Instagram 分享链接。当前为模拟分析；照片和文件识别尚未接入。", color = RecipeColors.Muted)
                            OutlinedTextField(state.input, model::setInput, Modifier.fillMaxWidth(), enabled = !state.loading,
                                label = { Text("链接 / 分享文字") }, minLines = 3)
                            TextButton(onClick = { model.setInput("https://www.xiaohongshu.com/explore/demo") }, enabled = !state.loading) { Text("填入示例链接") }
                        }
                        CreationMethod.MANUAL -> {
                            RecipeText("每行一种食材或一个步骤。菜谱暂存于本次会话，关闭进程后不会保留。", color = RecipeColors.Muted)
                            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("菜谱名称") })
                            OutlinedTextField(ingredients, { ingredients = it }, Modifier.fillMaxWidth(),
                                label = { Text("食材与数量") }, placeholder = { Text("意大利面 | 200 g\n番茄 | 2 个") }, minLines = 3)
                            OutlinedTextField(steps, { steps = it }, Modifier.fillMaxWidth(),
                                label = { Text("做法（每行一步）") }, minLines = 3)
                        }
                        CreationMethod.AI -> {
                            RecipeText("输入你的想法，体验生成流程。当前返回固定示例，不会调用真实 AI。", color = RecipeColors.Muted)
                            OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), enabled = !state.loading,
                                label = { Text("现有食材 / 饮食偏好") }, placeholder = { Text("例如：有番茄和意面，想做一份快手晚餐") }, minLines = 3)
                        }
                    }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.loading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        RecipeText(state.progress, 12, RecipeColors.Muted)
                    }
                    Button(onClick = {
                        when (method) {
                            CreationMethod.IMPORT -> model.analyze()
                            CreationMethod.MANUAL -> model.saveManual(title, ingredients, steps)
                            CreationMethod.AI -> model.generateDemo(prompt)
                        }
                    }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                        Text(if (method == CreationMethod.MANUAL) "添加菜谱" else "生成演示菜谱")
                    }
                    TextButton(onClick = model::closeMethod, modifier = Modifier.fillMaxWidth()) { Text("取消") }
                }
            }
        }
    }
}
