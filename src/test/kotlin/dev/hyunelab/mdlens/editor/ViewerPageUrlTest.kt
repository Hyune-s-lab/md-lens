package dev.hyunelab.mdlens.editor

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewerPageUrlTest {
    @Test
    fun `percent-encodes non-ascii characters and spaces like chromium url canonicalization`() {
        assertEquals(
            "file:///docs/260616-%EC%8A%A4%ED%94%84%EB%A7%81%20%EB%A0%88%ED%8D%BC%EB%9F%B0%EC%8A%A4.md",
            viewerPageUrl(Path.of("/docs/260616-스프링 레퍼런스.md")),
        )
    }

    @Test
    fun `keeps ascii paths unchanged`() {
        assertEquals(
            "file:///docs/AGENTS.md",
            viewerPageUrl(Path.of("/docs/AGENTS.md")),
        )
    }

    @Test
    fun `covers other scripts and url-hostile characters`() {
        assertEquals(
            "file:///docs/%E8%A8%AD%E8%A8%88%E3%83%A1%E3%83%A2%202026.md",
            viewerPageUrl(Path.of("/docs/設計メモ 2026.md")),
        )
        assertEquals(
            "file:///docs/%F0%9F%93%9D%20notes.md",
            viewerPageUrl(Path.of("/docs/📝 notes.md")),
        )
        assertEquals(
            "file:///docs/c%23%20100%25%20what%3F%20%5Bv2%5D.md",
            viewerPageUrl(Path.of("/docs/c# 100% what? [v2].md")),
        )
    }
}
