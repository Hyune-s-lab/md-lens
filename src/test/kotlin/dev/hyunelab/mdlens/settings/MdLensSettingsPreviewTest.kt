package dev.hyunelab.mdlens.settings

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MdLensSettingsPreviewTest {
    @Test
    fun testEveryLanguageSampleCoversTypographyAndTables() {
        for (sample in MdLensPreviewSample.entries.filter { it.languageCode.isNotEmpty() }) {
            val markdown = sample.markdown

            assertTrue(markdown.startsWith("# H1 — "), "$sample must start with an H1")
            assertTrue(markdown.contains("\n## H2 — "), "$sample must contain an H2")
            assertTrue(markdown.contains("\n### H3 — "), "$sample must contain an H3")
            assertTrue("\\*\\*[^*]+\\*\\*".toRegex().containsMatchIn(markdown), "$sample must contain bold text")
            assertTrue("(?<!`)`[^`\n]+`(?!`)".toRegex().containsMatchIn(markdown), "$sample must contain inline code")
            assertTrue(markdown.contains("| ---"), "$sample must contain a table separator row")
            assertTrue(markdown.contains("\n> "), "$sample must contain a blockquote")
            assertTrue(markdown.contains("> [!TIP]"), "$sample must contain a GitHub alert")
            assertTrue(markdown.contains("[^1]") && markdown.contains("\n[^1]: "), "$sample must contain a footnote")
            assertEquals(0, "```".toRegex().findAll(markdown).count(), "$sample must leave code blocks to the Code sample")
        }
    }

    @Test
    fun testCodeSampleCollectsEveryBundledSnippetType() {
        val markdown = MdLensPreviewSample.CODE.markdown

        assertTrue(markdown.startsWith("# H1 — "))
        for (language in listOf("kotlin", "python", "sql", "json", "yaml")) {
            assertTrue(markdown.contains("```$language"), "Code sample must contain a $language block")
        }
        assertEquals(10, "```".toRegex().findAll(markdown).count(), "Code sample must keep five fenced blocks")
    }

    @Test
    fun testMermaidSampleCollectsOnlyDiagrams() {
        val markdown = MdLensPreviewSample.MERMAID.markdown

        assertTrue(markdown.startsWith("# H1 — "))
        assertEquals(3, "```mermaid".toRegex().findAll(markdown).count())
        assertEquals(6, "```".toRegex().findAll(markdown).count())
        assertTrue(markdown.contains("flowchart"))
        assertTrue(markdown.contains("classDef"), "flowchart must showcase colored nodes")
        assertTrue(markdown.contains("sequenceDiagram"))
        assertTrue(markdown.contains("pie showData"))
    }

    @Test
    fun testSelectsTheSampleMatchingTheIdeLanguage() {
        assertEquals(MdLensPreviewSample.KOREAN, MdLensPreviewSample.forLocale(Locale.KOREAN))
        assertEquals(MdLensPreviewSample.JAPANESE, MdLensPreviewSample.forLocale(Locale.JAPAN))
        assertEquals(MdLensPreviewSample.CHINESE, MdLensPreviewSample.forLocale(Locale.SIMPLIFIED_CHINESE))
        assertEquals(MdLensPreviewSample.ENGLISH, MdLensPreviewSample.forLocale(Locale.US))
        assertEquals(MdLensPreviewSample.ENGLISH, MdLensPreviewSample.forLocale(Locale.GERMAN))
        assertEquals(MdLensPreviewSample.ENGLISH, MdLensPreviewSample.forLocale(Locale.ROOT))
    }
}
