package dev.hyunelab.mdlens.settings

import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MdLensSettingsTest {
    @Test
    fun `loads display names written by earlier appearance builds`() {
        val serializedState = Element("state").apply {
            addContent(Element("option").setAttribute("name", "theme").setAttribute("value", "Dark"))
            addContent(Element("option").setAttribute("name", "profile").setAttribute("value", "Spacious"))
        }
        val state = XmlSerializer.deserialize(
            serializedState,
            MdLensSettings.SettingsState::class.java,
        )
        val settings = MdLensSettings()

        settings.loadState(state)

        assertEquals(MdLensTheme.DARK, settings.theme)
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertTrue(settings.accentHeadings)
        assertFalse(settings.accentBold)
        assertFalse(settings.accentInlineCode)
    }

    @Test
    fun `uses light by default and reports actual theme changes`() {
        val settings = MdLensSettings()

        assertEquals(MdLensTheme.LIGHT, settings.theme)
        assertEquals("GitHub Light", MdLensTheme.LIGHT.toString())
        assertEquals("GitHub Dark", MdLensTheme.DARK.toString())
        assertEquals(MdLensProfile.COMPACT, settings.profile)
        assertEquals("", settings.bodyFontFamily)
        assertEquals("", settings.codeFontFamily)
        assertEquals(100, settings.fontScale)
        assertEquals(1152, settings.maxContentWidth)
        assertTrue(settings.useFullWidth)
        assertFalse(settings.accentHeadings)
        assertFalse(settings.accentBold)
        assertFalse(settings.accentInlineCode)
        assertTrue(
            settings.updateAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                bodyFontFamily = "Atkinson Hyperlegible",
                codeFontFamily = "JetBrains Mono",
                fontScale = 130,
                maxContentWidth = 1280,
                useFullWidth = true,
            ),
        )
        assertFalse(settings.accentHeadings)
        assertEquals(MdLensTheme.DARK, settings.theme)
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertEquals("Atkinson Hyperlegible", settings.bodyFontFamily)
        assertEquals("JetBrains Mono", settings.codeFontFamily)
        assertEquals(130, settings.fontScale)
        assertEquals(1280, settings.maxContentWidth)
        assertTrue(settings.useFullWidth)
        assertFalse(
            settings.updateAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                bodyFontFamily = "Atkinson Hyperlegible",
                codeFontFamily = "JetBrains Mono",
                fontScale = 130,
                maxContentWidth = 1280,
                useFullWidth = true,
            ),
        )
    }

    @Test
    fun `loads the persisted theme`() {
        val settings = MdLensSettings()

        settings.loadState(
            MdLensSettings.SettingsState(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                bodyFontFamily = "  Inter  ",
                codeFontFamily = "  JetBrains Mono  ",
                fontScale = 500,
                maxContentWidth = 5000,
                useFullWidth = false,
            ),
        )

        assertEquals(MdLensTheme.DARK, settings.theme)
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertEquals("Inter", settings.bodyFontFamily)
        assertEquals("JetBrains Mono", settings.codeFontFamily)
        assertEquals(180, settings.fontScale)
        assertEquals(1536, settings.maxContentWidth)
        assertFalse(settings.useFullWidth)
    }
}
