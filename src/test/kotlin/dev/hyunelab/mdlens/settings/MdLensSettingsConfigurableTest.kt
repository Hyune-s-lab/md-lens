package dev.hyunelab.mdlens.settings

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSlider
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
        val splitter = component as JBSplitter
        assertTrue(splitter.isVertical)
        assertEquals(0.38f, splitter.proportion, 0.001f)
        assertTrue(contains(splitter.secondComponent, preview.component))
        assertTrue(contains(component, preview.component))
        assertEquals(MdLensProfile.COMPACT, preview.appearances.last().profile)
        assertTrue(preview.appearances.last().useFullWidth)
        @Suppress("UNCHECKED_CAST")
        val themeField = findNamed(component, "theme") as ComboBox<MdLensTheme>
        @Suppress("UNCHECKED_CAST")
        val profileField = findNamed(component, "profile") as ComboBox<MdLensProfile>
        @Suppress("UNCHECKED_CAST")
        val bodyFontField = findNamed(component, "bodyFont") as ComboBox<String>
        @Suppress("UNCHECKED_CAST")
        val codeFontField = findNamed(component, "codeFont") as ComboBox<String>
        val accentHeadingsField = findNamed(component, "accentHeadings") as JBCheckBox
        val accentBoldField = findNamed(component, "accentBold") as JBCheckBox
        val accentInlineCodeField = findNamed(component, "accentInlineCode") as JBCheckBox
        val fontScaleField = findNamed(component, "fontScale") as JSlider
        val contentWidthField = findNamed(component, "contentWidth") as JSlider
        val contentWidthLabel = findNamed(component, "contentWidthLabel") as JBLabel
        assertFalse(accentHeadingsField.isSelected)
        assertFalse(accentBoldField.isSelected)
        assertFalse(accentInlineCodeField.isSelected)
        assertEquals("Default (system font)", bodyFontField.getItemAt(0))
        assertEquals("Default (system font)", codeFontField.getItemAt(0))
        assertEquals(
            listOf("Default (system font)", "Atkinson Hyperlegible"),
            bodyFontField.items(),
        )
        assertEquals(
            listOf("Default (system font)", "JetBrains Mono"),
            codeFontField.items(),
        )
        assertEquals(
            "If the selected font is unavailable, MdLens uses the default system font.",
            bodyFontField.toolTipText,
        )
        assertEquals(bodyFontField.toolTipText, codeFontField.toolTipText)
        assertEquals(1600, contentWidthField.maximum)
        assertEquals(1600, contentWidthField.value)
        assertEquals("Maximum content width: Full width", contentWidthLabel.text)
        for (value in listOf(768, 1152, 1536)) {
            contentWidthField.value = value
            assertEquals("Maximum content width: $value px", contentWidthLabel.text)
            assertEquals(value, preview.appearances.last().maxContentWidth)
            assertFalse(preview.appearances.last().useFullWidth)
        }
        contentWidthField.value = contentWidthField.maximum
        assertTrue(preview.appearances.last().useFullWidth)
        themeField.selectedItem = MdLensTheme.DARK
        profileField.selectedItem = MdLensProfile.SPACIOUS
        assertFalse(accentHeadingsField.isSelected)
        assertFalse(accentBoldField.isSelected)
        assertFalse(accentInlineCodeField.isSelected)
        accentHeadingsField.doClick()
        accentInlineCodeField.doClick()
        bodyFontField.selectedItem = "Atkinson Hyperlegible"
        codeFontField.selectedItem = "JetBrains Mono"
        fontScaleField.value = 130
        contentWidthField.value = 1280
        assertEquals("Maximum content width: 1280 px", contentWidthLabel.text)
        assertEquals(1280, preview.appearances.last().maxContentWidth)
        assertFalse(preview.appearances.last().useFullWidth)
        contentWidthField.value = contentWidthField.maximum
        assertEquals("Maximum content width: Full width", contentWidthLabel.text)

        assertEquals(
            MdLensAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                bodyFontFamily = "Atkinson Hyperlegible",
                codeFontFamily = "JetBrains Mono",
                fontScale = 130,
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
        assertEquals("Atkinson Hyperlegible", settings.bodyFontFamily)
        assertEquals("JetBrains Mono", settings.codeFontFamily)
        assertEquals(130, settings.fontScale)
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
                bodyFontFamily = "Legacy Reading Font",
                codeFontFamily = "Legacy Code Font",
                fontScale = 100,
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
        val bodyFontField = findNamed(component, "bodyFont") as ComboBox<String>
        @Suppress("UNCHECKED_CAST")
        val codeFontField = findNamed(component, "codeFont") as ComboBox<String>
        val contentWidthField = findNamed(component, "contentWidth") as JSlider
        val contentWidthLabel = findNamed(component, "contentWidthLabel") as JBLabel

        assertEquals("Legacy Reading Font", bodyFontField.selectedItem)
        assertEquals("Legacy Code Font", codeFontField.selectedItem)
        assertEquals(1152, contentWidthField.value)
        assertEquals("Maximum content width: 1152 px", contentWidthLabel.text)
        assertEquals(
            listOf("Default (system font)", "Legacy Reading Font", "Atkinson Hyperlegible"),
            bodyFontField.items(),
        )
        assertEquals(
            listOf("Default (system font)", "Legacy Code Font", "JetBrains Mono"),
            codeFontField.items(),
        )
        assertFalse(configurable.isModified)

        bodyFontField.selectedIndex = 0
        codeFontField.selectedIndex = 0
        assertTrue(configurable.isModified)
        configurable.reset()
        assertEquals("Legacy Reading Font", bodyFontField.selectedItem)
        assertEquals("Legacy Code Font", codeFontField.selectedItem)
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
