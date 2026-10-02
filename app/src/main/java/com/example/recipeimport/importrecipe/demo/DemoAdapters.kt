package com.example.recipeimport.importrecipe.demo

import com.example.recipeimport.ImportSource
import com.example.recipeimport.importrecipe.*
import kotlinx.coroutines.delay

class DemoPostExtractor : PostExtractor {
    override suspend fun extract(source: ImportSource): ResolvedPost {
        delay(400)
        return ResolvedPost(source,
            text = "番茄炒蛋，2 人份。番茄 2 个，鸡蛋 3 个，盐和食用油用量未提供。",
            images = listOf(PostMedia("photo-1", "demo://photo-1", "image/jpeg")),
            videos = listOf(PostMedia("video-1", "demo://video-1", "video/mp4")),
            isDemo = true,
        )
    }
}

class DemoMediaPreparer : MediaPreparer {
    override suspend fun prepare(post: ResolvedPost): AnalysisInput {
        delay(400)
        // These are illustrative references, not downloaded images, frames or a real transcript.
        return AnalysisInput(post.source, listOf(
            AnalysisEvidence("text-1", EvidenceKind.TEXT, text = post.text),
            AnalysisEvidence("image-1", EvidenceKind.IMAGE, text = "演示图片：番茄切块。", media = post.images.first()),
            AnalysisEvidence("frame-1", EvidenceKind.VIDEO_FRAME, text = "演示关键帧：蛋液炒至凝固后盛出。",
                media = PostMedia("frame-1", "demo://frame-1", "image/jpeg"), timestampSeconds = 12.0),
            AnalysisEvidence("audio-1", EvidenceKind.TRANSCRIPT, text = "演示转写：炒软番茄，再加入鸡蛋和盐炒匀。",
                timestampSeconds = 25.0),
        ), isDemo = true)
    }
}

class DemoVlmAnalyzer : VlmRecipeAnalyzer {
    override suspend fun analyze(input: AnalysisInput): ExtractedRecipe {
        delay(400)
        return ExtractedRecipe("番茄炒蛋（示例菜谱）", 2,
            ingredients = listOf(
                ExtractedIngredient("番茄", "2 个", listOf("text-1")),
                ExtractedIngredient("鸡蛋", "3 个", listOf("text-1")),
                ExtractedIngredient("食用油", null, listOf("text-1")),
                ExtractedIngredient("盐", null, listOf("text-1")),
            ),
            steps = listOf(
                ExtractedStep("番茄切块，鸡蛋打散。", listOf("image-1")),
                ExtractedStep("锅中加油，倒入蛋液炒至凝固，盛出备用。", listOf("frame-1")),
                ExtractedStep("炒软番茄，加入鸡蛋和盐，翻炒均匀。", listOf("audio-1")),
            ), isDemo = true,
        )
    }
}
