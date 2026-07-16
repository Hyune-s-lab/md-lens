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
        assertEquals(MdLensAccentColor.ORANGE, settings.accentHeadingsColor)
        assertEquals(MdLensAccentColor.GOLD, settings.accentBoldColor)
        assertEquals(MdLensAccentColor.GREEN, settings.accentInlineCodeColor)
    }

    @Test
    fun `loads persisted accent colors and ignores unknown color values`() {
        val serializedState = Element("state").apply {
            addContent(Element("option").setAttribute("name", "accentBoldColor").setAttribute("value", "pink"))
            addContent(Element("option").setAttribute("name", "accentInlineCodeColor").setAttribute("value", "no-such-color"))
        }
        val state = XmlSerializer.deserialize(
            serializedState,
            MdLensSettings.SettingsState::class.java,
        )
        val settings = MdLensSettings()

        settings.loadState(state)

        assertEquals(MdLensAccentColor.PINK, settings.accentBoldColor)
        assertEquals(MdLensAccentColor.GREEN, settings.accentInlineCodeColor)
    }

    @Test
    fun `syncs with the IDE by default and reports actual theme changes`() {
        val settings = MdLensSettings()

        assertEquals(MdLensTheme.SYNC, settings.theme)
        assertEquals("Sync with IDE", MdLensTheme.SYNC.toString())
        assertEquals("GitHub Light", MdLensTheme.LIGHT.toString())
        assertEquals("GitHub Dark", MdLensTheme.DARK.toString())
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertEquals("", settings.fontFamily)
        assertEquals(14, settings.fontSize)
        assertEquals(1152, settings.maxContentWidth)
        assertTrue(settings.useFullWidth)
        assertTrue(settings.accentHeadings)
        assertFalse(settings.accentBold)
        assertFalse(settings.accentInlineCode)
        assertEquals(MdLensAccentColor.ORANGE, settings.accentHeadingsColor)
        assertEquals(MdLensAccentColor.GOLD, settings.accentBoldColor)
        assertEquals(MdLensAccentColor.GREEN, settings.accentInlineCodeColor)
        assertTrue(
            settings.updateAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                fontFamily = "Atkinson Hyperlegible",
                fontSize = 16,
                maxContentWidth = 1280,
                useFullWidth = true,
            ),
        )
        assertFalse(settings.accentHeadings)
        assertEquals(MdLensTheme.DARK, settings.theme)
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertEquals("Atkinson Hyperlegible", settings.fontFamily)
        assertEquals(16, settings.fontSize)
        assertEquals(1280, settings.maxContentWidth)
        assertTrue(settings.useFullWidth)
        assertFalse(
            settings.updateAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                fontFamily = "Atkinson Hyperlegible",
                fontSize = 16,
                maxContentWidth = 1280,
                useFullWidth = true,
            ),
        )
        assertTrue(
            settings.updateAppearance(
                settings.appearance.copy(accentBoldColor = MdLensAccentColor.PURPLE),
            ),
        )
        assertEquals(MdLensAccentColor.PURPLE, settings.accentBoldColor)
    }

    @Test
    fun `resolves the sync theme to a concrete renderer theme`() {
        assertTrue(MdLensTheme.SYNC.resolveForRendering() in setOf(MdLensTheme.LIGHT, MdLensTheme.DARK))
        assertEquals(MdLensTheme.LIGHT, MdLensTheme.LIGHT.resolveForRendering())
        assertEquals(MdLensTheme.DARK, MdLensTheme.DARK.resolveForRendering())
    }

    @Test
    fun `loads the persisted theme`() {
        val settings = MdLensSettings()

        settings.loadState(
            MdLensSettings.SettingsState(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                fontFamily = "  Inter  ",
                fontSize = 500,
                maxContentWidth = 5000,
                useFullWidth = false,
            ),
        )

        assertEquals(MdLensTheme.DARK, settings.theme)
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertEquals("Inter", settings.fontFamily)
        assertEquals(24, settings.fontSize)
        assertEquals(1536, settings.maxContentWidth)
        assertFalse(settings.useFullWidth)
    }
}
