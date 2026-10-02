package com.example.recipeimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeimport.importrecipe.ImportFeature
import com.example.recipeimport.importrecipe.RecipeImportException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecipeScreen { MENU, ADD, DETAIL }
enum class CreationMethod { IMPORT, MANUAL, AI }

data class RecipeUiState(
    val input: String = "",
    val loading: Boolean = false,
    val progress: String = "",
    val error: String? = null,
    val recipe: Recipe? = null,
    val recipes: List<Recipe> = DemoRecipes.recipes,
    val screen: RecipeScreen = RecipeScreen.MENU,
    val method: CreationMethod? = null,
    val savedOnly: Boolean = false,
)

class RecipeViewModel(
    private val repository: RecipeRepository = ImportFeature.createDemoRepository(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecipeUiState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun setInput(value: String) { mutableState.update { it.copy(input = value, error = null) } }
    fun receiveShare(value: String) {
        cancel()
        mutableState.update { it.copy(input = value, screen = RecipeScreen.ADD, method = CreationMethod.IMPORT) }
    }
    fun openRecipe(recipe: Recipe) { mutableState.update { it.copy(recipe = recipe, screen = RecipeScreen.DETAIL) } }
    fun openAdd() { mutableState.update { it.copy(screen = RecipeScreen.ADD) } }
    fun selectTab(saved: Boolean) { mutableState.update { it.copy(savedOnly = saved) } }
    fun chooseMethod(method: CreationMethod) { mutableState.update { it.copy(method = method, error = null) } }
    fun closeMethod() {
        cancel()
        mutableState.update { it.copy(method = null, error = null) }
    }
    fun back() {
        if (state.value.method != null) closeMethod()
        else mutableState.update { it.copy(screen = RecipeScreen.MENU, savedOnly = false) }
    }
    private fun finish(recipe: Recipe) {
        mutableState.update {
            it.copy(loading = false, method = null, recipe = recipe, screen = RecipeScreen.DETAIL,
                recipes = listOf(recipe) + it.recipes, error = null)
        }
    }
    fun saveManual(title: String, ingredients: String, steps: String) {
        try { finish(ManualRecipeParser.create(title, ingredients, steps)) }
        catch (e: IllegalArgumentException) { mutableState.update { it.copy(error = e.message) } }
    }
    fun analyze() {
        if (state.value.loading) return
        val source = try { SourceParser.parse(state.value.input) } catch (e: IllegalArgumentException) {
            mutableState.update { it.copy(error = e.message) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null) }
        job = viewModelScope.launch {
            try {
                finish(repository.analyze(source) { progress -> mutableState.update { it.copy(progress = progress) } })
            } catch (e: CancellationException) { throw e }
            catch (e: RecipeImportException) { mutableState.update { it.copy(loading = false, error = e.message) } }
            catch (e: Exception) { mutableState.update { it.copy(loading = false, error = "分析失败，请重试。") } }
        }
    }
    fun generateDemo(prompt: String) {
        if (state.value.loading) return
        if (prompt.isBlank()) {
            mutableState.update { it.copy(error = "请先填写现有食材或饮食偏好。") }
            return
        }
        mutableState.update { it.copy(loading = true, error = null, progress = "正在生成演示菜谱…") }
        job = viewModelScope.launch {
            delay(800)
            finish(DemoRecipes.recipes.first().copy(
                id = java.util.UUID.randomUUID().toString(), isSaved = false,
                warnings = listOf("AI 生成演示：当前为固定示例，尚未根据你的输入调用模型。"),
            ))
        }
    }
    fun cancel() {
        job?.cancel()
        mutableState.update { it.copy(loading = false, progress = "") }
    }
}
