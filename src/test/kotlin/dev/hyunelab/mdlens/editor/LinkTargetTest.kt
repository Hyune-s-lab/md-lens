package dev.hyunelab.mdlens.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkTargetTest {
    @Test
    fun `routes web and mail links externally`() {
        val web = linkTarget("https://example.com/page?query=1")
        assertEquals("https://example.com/page?query=1", (web as LinkTarget.External).uri.toString())
        val mail = linkTarget("mailto:dev@example.com")
        assertEquals("mailto:dev@example.com", (mail as LinkTarget.External).uri.toString())
    }

    @Test
    fun `decodes file links into vfs urls with anchors`() {
        assertEquals(
            LinkTarget.LocalFile("file:///docs/스프링 정리.md", "캐시-전략"),
            linkTarget("file:///docs/%EC%8A%A4%ED%94%84%EB%A7%81%20%EC%A0%95%EB%A6%AC.md#%EC%BA%90%EC%8B%9C-%EC%A0%84%EB%9E%B5"),
        )
        assertEquals(
            LinkTarget.LocalFile("file:///docs/AGENTS.md", null),
            linkTarget("file:///docs/AGENTS.md"),
        )
        assertEquals(
            LinkTarget.LocalFile("file:///docs/AGENTS.md", null),
            linkTarget("file:///docs/AGENTS.md#"),
        )
    }

    @Test
    fun `ignores unknown schemes and malformed links`() {
        assertNull(linkTarget("javascript:alert(1)"))
        assertNull(linkTarget("jetbrains://idea/settings"))
        assertNull(linkTarget("http://["))
        assertNull(linkTarget("file://"))
    }
}
