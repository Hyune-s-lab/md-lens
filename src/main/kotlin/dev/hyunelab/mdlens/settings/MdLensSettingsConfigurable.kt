package dev.hyunelab.mdlens.settings

import com.intellij.DynamicBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.ui.ColorIcon
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GraphicsEnvironment
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.util.Locale
import javax.swing.Box
import javax.swing.ButtonGroup
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JToggleButton
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MdLensSettingsConfigurable internal constructor(
    private val settings: MdLensSettings,
    private val notifySettingsChanged: () -> Unit,
    private val availableFontFamilies: () -> List<String>,
    private val previewFactory: () -> MdLensSettingsPreview?,
) : Configurable {
    constructor() : this(
        MdLensSettings.getInstance(),
        ::publishSettingsChanged,
        ::systemFontFamilies,
        ::createMdLensSettingsPreview,
    )

    private var themeField: ComboBox<MdLensTheme>? = null
    private var profileField: ComboBox<MdLensProfile>? = null
    private var accentHeadingsField: JBCheckBox? = null
    private var accentBoldField: JBCheckBox? = null
    private var accentInlineCodeField: JBCheckBox? = null
    private var accentHeadingsColorField: ComboBox<MdLensAccentColor>? = null
    private var accentBoldColorField: ComboBox<MdLensAccentColor>? = null
    private var accentInlineCodeColorField: ComboBox<MdLensAccentColor>? = null
    private var fontField: ComboBox<String>? = null
    private var fontSizeField: JBTextField? = null
    private var contentWidthField: ComboBox<Int>? = null
    private var preview: MdLensSettingsPreview? = null

    override fun getDisplayName(): String = "MdLens"

    override fun createComponent(): JComponent {
        themeField = ComboBox(MdLensTheme.entries.toTypedArray()).apply {
            name = "theme"
            addActionListener { refreshPreview() }
        }
        profileField = ComboBox(MdLensProfile.entries.toTypedArray()).apply {
            name = "profile"
            addActionListener { refreshPreview() }
        }
        accentHeadingsField = accentCheckBox("accentHeadings", "Headings")
        accentBoldField = accentCheckBox("accentBold", "Bold")
        accentInlineCodeField = accentCheckBox("accentInlineCode", "Inline code")
        accentHeadingsColorField = accentColorComboBox("accentHeadingsColor")
        accentBoldColorField = accentColorComboBox("accentBoldColor")
        accentInlineCodeColorField = accentColorComboBox("accentInlineCodeColor")
        bindAccentColorEnablement()

        val installedFonts = availableFontFamilies()
        fontField = ComboBox(
            fontChoices(installedFonts, RECOMMENDED_FONTS, settings.fontFamily),
        ).apply {
            name = "font"
            toolTipText = FONT_FALLBACK_TOOLTIP
            addActionListener { refreshPreview() }
        }
        fontSizeField = JBTextField(settings.fontSize.toString()).apply {
            name = "fontSize"
            columns = 4
            document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = refreshPreview()
                override fun removeUpdate(e: DocumentEvent) = refreshPreview()
                override fun changedUpdate(e: DocumentEvent) = refreshPreview()
            })
        }
        contentWidthField = ComboBox(CONTENT_WIDTH_CHOICES.toTypedArray()).apply {
            name = "contentWidth"
            renderer = textListCellRenderer { value ->
                if (value == FULL_WIDTH_CHOICE) "Full width" else "$value px"
            }
            addActionListener { refreshPreview() }
        }
        // One line per highlight group, with the color combos sharing a column so the
        // swatches line up regardless of the checkbox label widths.
        val highlightGroupsRow = JPanel(GridBagLayout()).apply {
            val groups = listOf(
                requireNotNull(accentHeadingsField) to requireNotNull(accentHeadingsColorField),
                requireNotNull(accentBoldField) to requireNotNull(accentBoldColorField),
                requireNotNull(accentInlineCodeField) to requireNotNull(accentInlineCodeColorField),
            )
            for ((index, group) in groups.withIndex()) {
                val (checkBox, colorField) = group
                add(
                    checkBox,
                    GridBagConstraints().apply {
                        gridx = 0
                        gridy = index
                        anchor = GridBagConstraints.LINE_START
                        insets = Insets(0, 0, if (index < groups.lastIndex) 4 else 0, 8)
                    },
                )
                add(
                    colorField,
                    GridBagConstraints().apply {
                        gridx = 1
                        gridy = index
                        anchor = GridBagConstraints.LINE_START
                        insets = Insets(0, 0, if (index < groups.lastIndex) 4 else 0, 0)
                    },
                )
            }
        }
        val fontSizeCluster = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).apply {
            add(requireNotNull(fontField))
            add(Box.createHorizontalStrut(8))
            add(JBLabel("Size:"))
            add(Box.createHorizontalStrut(4))
            add(requireNotNull(fontSizeField))
            add(Box.createHorizontalStrut(4))
            add(JBLabel("px"))
        }
        // A four-column grid keeps both label columns vertically aligned; packing the
        // second label into a FlowLayout row loses that shared edge and looks cluttered.
        val form = JPanel(GridBagLayout())
        fun cell(x: Int, y: Int, configure: GridBagConstraints.() -> Unit = {}) =
            GridBagConstraints().apply {
                gridx = x
                gridy = y
                anchor = GridBagConstraints.LINE_START
                insets = Insets(0, 0, 4, 8)
                configure()
            }

        val secondLabelInsets = Insets(0, 56, 4, 8)
        form.add(JBLabel("Theme:"), cell(0, 0))
        form.add(requireNotNull(themeField), cell(1, 0))
        form.add(JBLabel("Density:"), cell(2, 0) { insets = secondLabelInsets })
        form.add(requireNotNull(profileField), cell(3, 0))
        form.add(
            ActionLink("Restore defaults") { restoreDefaults() }.apply {
                name = "restoreDefaults"
            },
            cell(4, 0) {
                anchor = GridBagConstraints.LINE_END
                weightx = 1.0
                insets = Insets(0, 24, 4, 0)
            },
        )
        form.add(JBLabel("Font:"), cell(0, 1))
        form.add(fontSizeCluster, cell(1, 1))
        form.add(JBLabel("Width:"), cell(2, 1) { insets = secondLabelInsets })
        form.add(requireNotNull(contentWidthField), cell(3, 1))
        form.add(JBLabel("Highlight:"), cell(0, 2) { anchor = GridBagConstraints.FIRST_LINE_START })
        form.add(highlightGroupsRow, cell(1, 2) { gridwidth = 4 })
        form.also { reset() }

        preview = previewFactory()
        val currentPreview = preview ?: return form
        val initialSample = MdLensPreviewSample.forLocale(ideLocale())
        currentPreview.selectSample(initialSample)
        refreshPreview()
        val previewPanel = JPanel(BorderLayout(0, 4)).apply {
            add(previewSampleTabBar(currentPreview, initialSample), BorderLayout.NORTH)
            add(currentPreview.component, BorderLayout.CENTER)
        }
        return JPanel(BorderLayout(0, 12)).apply {
            add(form, BorderLayout.NORTH)
            add(previewPanel, BorderLayout.CENTER)
        }
    }

    private fun previewSampleTabBar(
        preview: MdLensSettingsPreview,
        initialSample: MdLensPreviewSample,
    ): JComponent {
        val group = ButtonGroup()
        val bar = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).apply {
            name = "previewSampleTabs"
        }
        var previousWasLanguage = false
        for (sample in MdLensPreviewSample.entries) {
            val isLanguage = sample.languageCode.isNotEmpty()
            val tab = JToggleButton(sample.displayName).apply {
                name = "previewSample${sample.name}"
                isSelected = sample == initialSample
                addActionListener { preview.selectSample(sample) }
            }
            if (previousWasLanguage && !isLanguage) {
                bar.add(Box.createHorizontalStrut(8))
                bar.add(
                    JSeparator(SwingConstants.VERTICAL).apply {
                        name = "previewSampleSeparator"
                        preferredSize = Dimension(1, tab.preferredSize.height * 2 / 3)
                    },
                )
                bar.add(Box.createHorizontalStrut(8))
            }
            group.add(tab)
            bar.add(tab)
            previousWasLanguage = isLanguage
        }
        return bar
    }

    private fun restoreDefaults() {
        val defaults = MdLensSettings().appearance
        themeField?.selectedItem = defaults.theme
        profileField?.selectedItem = defaults.profile
        accentHeadingsField?.isSelected = defaults.accentHeadings
        accentBoldField?.isSelected = defaults.accentBold
        accentInlineCodeField?.isSelected = defaults.accentInlineCode
        accentHeadingsColorField?.selectedItem = defaults.accentHeadingsColor
        accentBoldColorField?.selectedItem = defaults.accentBoldColor
        accentInlineCodeColorField?.selectedItem = defaults.accentInlineCodeColor
        fontField?.selectedItem = displayedFont(defaults.fontFamily)
        fontSizeField?.text = defaults.fontSize.toString()
        contentWidthField?.selectedItem = if (defaults.useFullWidth) {
            FULL_WIDTH_CHOICE
        } else {
            defaults.maxContentWidth
        }
        bindAccentColorEnablement()
        refreshPreview()
    }

    override fun isModified(): Boolean {
        val useFullWidth = selectedUseFullWidth()
        return themeField?.selectedItem != settings.theme ||
            profileField?.selectedItem != settings.profile ||
            accentHeadingsField?.isSelected != settings.accentHeadings ||
            accentBoldField?.isSelected != settings.accentBold ||
            accentInlineCodeField?.isSelected != settings.accentInlineCode ||
            accentHeadingsColorField?.selectedItem != settings.accentHeadingsColor ||
            accentBoldColorField?.selectedItem != settings.accentBoldColor ||
            accentInlineCodeColorField?.selectedItem != settings.accentInlineCodeColor ||
            selectedFont(fontField) != settings.fontFamily ||
            selectedFontSize() != settings.fontSize ||
            useFullWidth != settings.useFullWidth ||
            (!useFullWidth && contentWidthField?.selectedItem != settings.maxContentWidth)
    }

    override fun apply() {
        if (settings.updateAppearance(selectedAppearance())) {
            notifySettingsChanged()
        }
    }

    override fun reset() {
        themeField?.selectedItem = settings.theme
        profileField?.selectedItem = settings.profile
        accentHeadingsField?.isSelected = settings.accentHeadings
        accentBoldField?.isSelected = settings.accentBold
        accentInlineCodeField?.isSelected = settings.accentInlineCode
        accentHeadingsColorField?.selectedItem = settings.accentHeadingsColor
        accentBoldColorField?.selectedItem = settings.accentBoldColor
        accentInlineCodeColorField?.selectedItem = settings.accentInlineCodeColor
        fontField?.selectedItem = displayedFont(settings.fontFamily)
        fontSizeField?.text = settings.fontSize.toString()
        contentWidthField?.selectedItem = if (settings.useFullWidth) {
            FULL_WIDTH_CHOICE
        } else {
            settings.maxContentWidth
        }
        bindAccentColorEnablement()
        refreshPreview()
    }

    override fun disposeUIResources() {
        preview?.dispose()
        preview = null
        themeField = null
        profileField = null
        accentHeadingsField = null
        accentBoldField = null
        accentInlineCodeField = null
        accentHeadingsColorField = null
        accentBoldColorField = null
        accentInlineCodeColorField = null
        fontField = null
        fontSizeField = null
        contentWidthField = null
    }

    private fun fontChoices(
        installedFonts: List<String>,
        recommendedFonts: List<String>,
        selectedFont: String,
    ): Array<String> = buildList {
        add(DEFAULT_FONT)
        if (selectedFont.isNotEmpty()) {
            add(selectedFont)
        }
        addAll(recommendedFonts.mapNotNull { recommendation ->
            installedFonts.firstOrNull { it.equals(recommendation, ignoreCase = true) }
        })
    }.distinctBy { it.lowercase() }.toTypedArray()

    private fun refreshPreview() {
        preview?.render(selectedAppearance())
    }

    private fun selectedAppearance(): MdLensAppearance = MdLensAppearance(
        theme = themeField?.selectedItem as? MdLensTheme ?: settings.theme,
        profile = profileField?.selectedItem as? MdLensProfile ?: settings.profile,
        fontFamily = selectedFont(fontField),
        fontSize = selectedFontSize(),
        maxContentWidth = selectedMaxContentWidth(),
        useFullWidth = selectedUseFullWidth(),
        accentHeadings = accentHeadingsField?.isSelected ?: settings.accentHeadings,
        accentBold = accentBoldField?.isSelected ?: settings.accentBold,
        accentInlineCode = accentInlineCodeField?.isSelected ?: settings.accentInlineCode,
        accentHeadingsColor = accentHeadingsColorField?.selectedItem as? MdLensAccentColor
            ?: settings.accentHeadingsColor,
        accentBoldColor = accentBoldColorField?.selectedItem as? MdLensAccentColor
            ?: settings.accentBoldColor,
        accentInlineCodeColor = accentInlineCodeColorField?.selectedItem as? MdLensAccentColor
            ?: settings.accentInlineCodeColor,
    )

    private fun accentCheckBox(fieldName: String, label: String): JBCheckBox =
        JBCheckBox(label).apply {
            name = fieldName
            addActionListener {
                bindAccentColorEnablement()
                refreshPreview()
            }
        }

    private fun accentColorComboBox(fieldName: String): ComboBox<MdLensAccentColor> =
        ComboBox(MdLensAccentColor.entries.toTypedArray()).apply {
            name = fieldName
            renderer = AccentColorCellRenderer()
            addActionListener { refreshPreview() }
        }

    private fun bindAccentColorEnablement() {
        accentHeadingsColorField?.isEnabled = accentHeadingsField?.isSelected == true
        accentBoldColorField?.isEnabled = accentBoldField?.isSelected == true
        accentInlineCodeColorField?.isEnabled = accentInlineCodeField?.isSelected == true
    }

    private fun selectedFontSize(): Int {
        return fontSizeField?.text?.trim()?.toIntOrNull()?.coerceIn(
            MdLensSettings.MIN_FONT_SIZE,
            MdLensSettings.MAX_FONT_SIZE,
        ) ?: settings.fontSize
    }

    private fun selectedUseFullWidth(): Boolean =
        (contentWidthField?.selectedItem as? Int) == FULL_WIDTH_CHOICE

    private fun selectedMaxContentWidth(): Int {
        if (selectedUseFullWidth()) {
            return settings.maxContentWidth
        }
        return (contentWidthField?.selectedItem as? Int) ?: settings.maxContentWidth
    }

    private companion object {
        const val DEFAULT_FONT = "Default (system font)"
        const val FULL_WIDTH_CHOICE = -1
        const val FONT_FALLBACK_TOOLTIP =
            "If the selected font is unavailable, MdLens uses the default system font."
        val CONTENT_WIDTH_CHOICES: List<Int> =
            (MdLensSettings.MIN_CONTENT_WIDTH..MdLensSettings.MAX_CONTENT_WIDTH step 128).toList() + FULL_WIDTH_CHOICE
        val RECOMMENDED_FONTS = listOf(
            "Pretendard",
            "Inter",
            "Atkinson Hyperlegible",
            "JetBrains Mono",
            "D2Coding",
            "Noto Serif",
        )

        fun displayedFont(fontFamily: String): String = fontFamily.ifEmpty { DEFAULT_FONT }

        fun selectedFont(field: ComboBox<String>?): String =
            (field?.selectedItem as? String).orEmpty().takeUnless { it == DEFAULT_FONT }.orEmpty()

        fun systemFontFamilies(): List<String> =
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
                .sortedWith(String.CASE_INSENSITIVE_ORDER)

        fun publishSettingsChanged() {
            ApplicationManager.getApplication().messageBus
                .syncPublisher(MdLensSettingsListener.TOPIC)
                .settingsChanged()
        }

        fun ideLocale(): Locale =
            runCatching { DynamicBundle.getLocale() }.getOrDefault(Locale.getDefault())
    }

    private class AccentColorCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            val accentColor = value as? MdLensAccentColor ?: return this
            text = accentColor.displayName
            val hex = if (runCatching { JBColor.isBright() }.getOrDefault(true)) {
                accentColor.lightHex
            } else {
                accentColor.darkHex
            }
            icon = ColorIcon(SWATCH_SIZE, Color.decode(hex))
            return this
        }

        private companion object {
            const val SWATCH_SIZE = 12
        }
    }
}
