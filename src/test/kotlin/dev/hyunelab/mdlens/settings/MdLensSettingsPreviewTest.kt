package dev.hyunelab.mdlens.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MdLensSettingsPreviewTest {
    @Test
    fun testSampleCoversHierarchyAndDistinctCodeBlocks() {
        val sample = MARKDOWN_NEAT_SETTINGS_PREVIEW_SAMPLE

        assertTrue(sample.startsWith("# H1 — Reading Preview"))
        assertTrue(sample.contains("\n## H2 — Typography hierarchy"))
        assertTrue(sample.contains("\n### H3 — Text styles"))
        assertTrue(sample.contains("\n## H2 — TypeScript example"))
        assertTrue(sample.contains("\n### H3 — Configuration example"))
        assertTrue(sample.contains("**bold text**"))
        assertTrue(sample.contains("`inline code`"))
        assertTrue(sample.contains("```typescript"))
        assertTrue(sample.contains("```yaml"))
        assertEquals(4, "```".toRegex().findAll(sample).count())
    }
}
