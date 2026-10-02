package com.example.recipeimport.importrecipe

import com.example.recipeimport.RecipeRepository
import com.example.recipeimport.importrecipe.demo.DemoMediaPreparer
import com.example.recipeimport.importrecipe.demo.DemoPostExtractor
import com.example.recipeimport.importrecipe.demo.DemoVlmAnalyzer

/** Change wiring here without changing menu/detail UI. Default remains offline and free. */
object ImportFeature {
    fun createDemoRepository(): RecipeRepository = ImportRecipePipeline(
        extractor = DemoPostExtractor(),
        mediaPreparer = DemoMediaPreparer(),
        analyzer = DemoVlmAnalyzer(),
        demoMode = true,
    )
}
