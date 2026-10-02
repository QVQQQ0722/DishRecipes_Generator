package com.example.recipeimport

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceParserTest {
    @Test fun acceptsChineseShareText() {
        assertEquals(Platform.XIAOHONGSHU, SourceParser.parse("今天做饭 https://xhslink.com/a/abc，复制打开").platform)
    }
    @Test fun acceptsInstagramReel() {
        assertEquals(Platform.INSTAGRAM, SourceParser.parse("https://www.instagram.com/reel/abc/").platform)
    }
    @Test fun acceptsBilibiliAndShortLink() {
        assertEquals(Platform.BILIBILI, SourceParser.parse("https://www.bilibili.com/video/BV123/").platform)
        assertEquals(Platform.BILIBILI, SourceParser.parse("分享 https://b23.tv/example").platform)
    }
    @Test fun acceptsTikTokAndShortLink() {
        assertEquals(Platform.TIKTOK, SourceParser.parse("https://www.tiktok.com/@cook/video/123").platform)
        assertEquals(Platform.TIKTOK, SourceParser.parse("https://vm.tiktok.com/example/").platform)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsSpoofedTikTokHost() {
        SourceParser.parse("https://tiktok.com.evil.example/video/123")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsSpoofedHost() {
        SourceParser.parse("https://instagram.com.evil.example/reel/abc")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsCredentials() {
        SourceParser.parse("https://evil.example@instagram.com/reel/abc")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsPlainText() {
        SourceParser.parse("番茄炒蛋")
    }
}
