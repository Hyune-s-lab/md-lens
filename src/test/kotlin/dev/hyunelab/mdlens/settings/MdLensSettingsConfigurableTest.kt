package dev.hyunelab.mdlens.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MdLensSettingsConfigurableTest {
    @Test
    fun testPreviewsAndAppliesAppearanceBeforeNotifyingOpenViewers() {
        val settings = MdLensSettings()
        var notifications = 0
        val preview = RecordingSettingsPreview()

        val configurable = MdLensSettingsConfigurable(
            settings = settings,
            notifySettingsChanged = { notifications += 1 },
            availableFontFamilies = {
                listOf("Arial", "Atkinson Hyperlegible", "Comic Sans MS", "JetBrains Mono")
            },
            previewFactory = { preview },
        )
        val component = configurable.createComponent()
        val layout = (component as JPanel).layout as BorderLayout
        assertEquals(preview.component, layout.getLayoutComponent(BorderLayout.CENTER))
        assertTrue(contains(component, preview.component))
        assertEquals(MdLensProfile.COMPACT, preview.appearances.last().profile)
        assertTrue(preview.appearances.last().useFullWidth)
        @Suppress("UNCHECKED_CAST")
        val themeField = findNamed(component, "theme") as ComboBox<MdLensTheme>
        @Suppress("UNCHECKED_CAST")
        val profileField = findNamed(component, "profile") as ComboBox<MdLensProfile>
        @Suppress("UNCHECKED_CAST")
        val fontField = findNamed(component, "font") as ComboBox<String>
        val accentHeadingsField = findNamed(component, "accentHeadings") as JBCheckBox
        val accentBoldField = findNamed(component, "accentBold") as JBCheckBox
        val accentInlineCodeField = findNamed(component, "accentInlineCode") as JBCheckBox
        val fontSizeField = findNamed(component, "fontSize") as JBTextField
        @Suppress("UNCHECKED_CAST")
        val contentWidthField = findNamed(component, "contentWidth") as ComboBox<Int>
        assertFalse(accentHeadingsField.isSelected)
        assertFalse(accentBoldField.isSelected)
        assertFalse(accentInlineCodeField.isSelected)
        assertEquals("Default (system font)", fontField.getItemAt(0))
        assertEquals(
            listOf("Default (system font)", "Atkinson Hyperlegible", "JetBrains Mono"),
            fontField.items(),
        )
        assertEquals(-1, contentWidthField.selectedItem)
        assertTrue(preview.appearances.last().useFullWidth)
        for (value in listOf(768, 1152, 1536)) {
            contentWidthField.selectedItem = value
            assertEquals(value, preview.appearances.last().maxContentWidth)
            assertFalse(preview.appearances.last().useFullWidth)
        }
        contentWidthField.selectedItem = -1
        assertTrue(preview.appearances.last().useFullWidth)
        themeField.selectedItem = MdLensTheme.DARK
        profileField.selectedItem = MdLensProfile.SPACIOUS
        assertFalse(accentHeadingsField.isSelected)
        assertFalse(accentBoldField.isSelected)
        assertFalse(accentInlineCodeField.isSelected)
        accentHeadingsField.doClick()
        accentInlineCodeField.doClick()
        fontField.selectedItem = "Atkinson Hyperlegible"
        fontSizeField.text = "16"
        contentWidthField.selectedItem = 1280
        assertEquals(1280, preview.appearances.last().maxContentWidth)
        assertFalse(preview.appearances.last().useFullWidth)
        contentWidthField.selectedItem = -1
        assertTrue(preview.appearances.last().useFullWidth)

        assertEquals(
            MdLensAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                fontFamily = "Atkinson Hyperlegible",
                fontSize = 16,
                maxContentWidth = 1152,
                useFullWidth = true,
                accentHeadings = true,
                accentBold = false,
                accentInlineCode = true,
            ),
            preview.appearances.last(),
        )
        assertTrue(configurable.isModified)
        configurable.apply()
        assertEquals(MdLensTheme.DARK, settings.theme)
        assertEquals(MdLensProfile.SPACIOUS, settings.profile)
        assertEquals("Atkinson Hyperlegible", settings.fontFamily)
        assertEquals(16, settings.fontSize)
        assertEquals(1152, settings.maxContentWidth)
        assertTrue(settings.useFullWidth)
        assertTrue(settings.accentHeadings)
        assertFalse(settings.accentBold)
        assertTrue(settings.accentInlineCode)
        assertEquals(1, notifications)
        assertFalse(configurable.isModified)

        configurable.disposeUIResources()
        assertTrue(preview.disposed)
    }

    @Test
    fun testKeepsStoredFontsOutsideTheRecommendedInstalledChoices() {
        val settings = MdLensSettings().apply {
            updateAppearance(
                theme = MdLensTheme.LIGHT,
                profile = MdLensProfile.COMPACT,
                fontFamily = "Legacy Reading Font",
                fontSize = 14,
                maxContentWidth = 1152,
                useFullWidth = false,
            )
        }
        val configurable = MdLensSettingsConfigurable(
            settings = settings,
            notifySettingsChanged = {},
            availableFontFamilies = { listOf("Atkinson Hyperlegible", "JetBrains Mono") },
            previewFactory = { null },
        )
        val component = configurable.createComponent()
        @Suppress("UNCHECKED_CAST")
        val fontField = findNamed(component, "font") as ComboBox<String>
        @Suppress("UNCHECKED_CAST")
        val contentWidthField = findNamed(component, "contentWidth") as ComboBox<Int>

        assertEquals("Legacy Reading Font", fontField.selectedItem)
        assertEquals(1152, contentWidthField.selectedItem)
        assertEquals(
            listOf("Default (system font)", "Legacy Reading Font", "Atkinson Hyperlegible", "JetBrains Mono"),
            fontField.items(),
        )
        assertFalse(configurable.isModified)

        fontField.selectedIndex = 0
        assertTrue(configurable.isModified)
        configurable.reset()
        assertEquals("Legacy Reading Font", fontField.selectedItem)
        assertFalse(configurable.isModified)

        configurable.disposeUIResources()
    }

    private fun findNamed(component: Component, name: String): Component {
        if (component.name == name) {
            return component
        }
        if (component is Container) {
            for (child in component.components) {
                runCatching { return findNamed(child, name) }
            }
        }
        error("Component $name not found")
    }

    private fun contains(root: Component, target: Component): Boolean =
        root === target || (root is Container && root.components.any { contains(it, target) })

    private fun ComboBox<String>.items(): List<String> =
        (0 until itemCount).map(::getItemAt)

    private class RecordingSettingsPreview : MdLensSettingsPreview {
        override val component: JComponent = JPanel()
        val appearances = mutableListOf<MdLensAppearance>()
        var disposed = false

        override fun render(appearance: MdLensAppearance) {
            appearances += appearance
        }

        override fun dispose() {
            disposed = true
        }
    }
}
