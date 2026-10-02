package com.example.recipeimport.importrecipe

import com.example.recipeimport.ImportSource
import com.example.recipeimport.Platform
import java.net.URI

/** Identifies a share link; this does NOT download a post or follow redirects. */
object LinkParser {
    fun parse(text: String): ImportSource {
        val links = Regex("https?://[^\\s<>\"，。！？、）】]+", RegexOption.IGNORE_CASE)
            .findAll(text).map { it.value.trimEnd('.', ',', ')', ']', ';') }
        for (url in links) {
            val uri = runCatching { URI(url) }.getOrNull() ?: continue
            val host = uri.host?.lowercase() ?: continue
            if (uri.userInfo != null || uri.port !in listOf(-1, 80, 443)) continue
            fun belongsTo(domain: String) = host == domain || host.endsWith(".$domain")
            val platform = when {
                belongsTo("xiaohongshu.com") || host == "xhslink.com" -> Platform.XIAOHONGSHU
                belongsTo("instagram.com") -> Platform.INSTAGRAM
                belongsTo("bilibili.com") || host == "b23.tv" -> Platform.BILIBILI
                belongsTo("tiktok.com") -> Platform.TIKTOK
                else -> continue
            }
            return ImportSource(url, platform)
        }
        throw IllegalArgumentException("请粘贴小红书、Bilibili、TikTok 或 Instagram 的链接或分享文字。")
    }
}
