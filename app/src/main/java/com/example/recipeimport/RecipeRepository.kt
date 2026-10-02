package com.example.recipeimport

/** App-facing contract. Model credentials and real media processing belong on a backend. */
interface RecipeRepository {
    suspend fun analyze(source: ImportSource, onProgress: (String) -> Unit): Recipe
}

/** Compatibility alias for existing callers; demo stages now live in importrecipe/demo. */
class DemoRecipeRepository : RecipeRepository by
    com.example.recipeimport.importrecipe.ImportFeature.createDemoRepository()
