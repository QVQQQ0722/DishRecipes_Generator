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
