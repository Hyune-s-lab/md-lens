package dev.hyunelab.mdlens.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.options.Configurable
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GraphicsEnvironment
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel
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
            renderer = SimpleListCellRenderer.create { label, value, _ ->
                label.text = if (value == FULL_WIDTH_CHOICE) "Full width" else "$value px"
            }
            addActionListener { refreshPreview() }
        }
        // FlowLayout applies hgap before the first component too, which breaks the
        // column alignment against plain fields; keep hgap at 0 and use struts instead.
        val highlightGroupsRow = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).apply {
            add(requireNotNull(accentHeadingsField))
            add(Box.createHorizontalStrut(12))
            add(requireNotNull(accentBoldField))
            add(Box.createHorizontalStrut(12))
            add(requireNotNull(accentInlineCodeField))
        }
        val fontRow = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0)).apply {
            add(requireNotNull(fontField))
            add(Box.createHorizontalStrut(8))
            add(JBLabel("Size:"))
            add(Box.createHorizontalStrut(4))
            add(requireNotNull(fontSizeField))
            add(Box.createHorizontalStrut(4))
            add(JBLabel("px"))
        }
        val form = JPanel(GridBagLayout())
        val labelConstraints = GridBagConstraints().apply {
            gridx = 0
            anchor = GridBagConstraints.LINE_START
            insets = Insets(0, 0, 4, 8)
        }
        val fieldConstraints = GridBagConstraints().apply {
            gridx = 1
            anchor = GridBagConstraints.LINE_START
            weightx = 0.0
            fill = GridBagConstraints.NONE
            insets = Insets(0, 0, 4, 0)
        }
        val rows = listOf(
            JBLabel("Theme:") to requireNotNull(themeField),
            JBLabel("Density:") to requireNotNull(profileField),
            JBLabel("Highlight:") to highlightGroupsRow,
            JBLabel("Font:") to fontRow,
            JBLabel("Content width:") to requireNotNull(contentWidthField),
        )
        for ((index, pair) in rows.withIndex()) {
            val (label, component) = pair
            labelConstraints.gridy = index
            fieldConstraints.gridy = index
            form.add(label, labelConstraints)
            form.add(component, fieldConstraints)
        }
        val fillerConstraints = GridBagConstraints().apply {
            gridx = 2
            gridy = 0
            gridheight = GridBagConstraints.REMAINDER
            weightx = 1.0
            fill = GridBagConstraints.BOTH
        }
        form.add(JPanel(), fillerConstraints)
        form.also { reset() }

        preview = previewFactory()
        val currentPreview = preview ?: return form
        refreshPreview()
        return JPanel(BorderLayout(0, 12)).apply {
            add(form, BorderLayout.NORTH)
            add(currentPreview.component, BorderLayout.CENTER)
        }
    }

    override fun isModified(): Boolean {
        val useFullWidth = selectedUseFullWidth()
        return themeField?.selectedItem != settings.theme ||
            profileField?.selectedItem != settings.profile ||
            accentHeadingsField?.isSelected != settings.accentHeadings ||
            accentBoldField?.isSelected != settings.accentBold ||
            accentInlineCodeField?.isSelected != settings.accentInlineCode ||
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
        fontField?.selectedItem = displayedFont(settings.fontFamily)
        fontSizeField?.text = settings.fontSize.toString()
        contentWidthField?.selectedItem = if (settings.useFullWidth) {
            FULL_WIDTH_CHOICE
        } else {
            settings.maxContentWidth
        }
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
    )

    private fun accentCheckBox(fieldName: String, label: String): JBCheckBox =
        JBCheckBox(label).apply {
            name = fieldName
            addActionListener { refreshPreview() }
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
    }
}
