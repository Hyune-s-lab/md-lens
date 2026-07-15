package dev.hyunelab.mdlens.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.ui.FormBuilder
import java.awt.FlowLayout
import java.awt.GraphicsEnvironment
import javax.swing.JComponent
import javax.swing.JPanel

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
    private var bodyFontField: ComboBox<String>? = null
    private var codeFontField: ComboBox<String>? = null
    private var fontScaleField: ComboBox<Int>? = null
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
        bodyFontField = ComboBox(
            fontChoices(installedFonts, RECOMMENDED_BODY_FONTS, settings.bodyFontFamily),
        ).apply {
            name = "bodyFont"
            toolTipText = FONT_FALLBACK_TOOLTIP
            addActionListener { refreshPreview() }
        }
        codeFontField = ComboBox(
            fontChoices(installedFonts, RECOMMENDED_CODE_FONTS, settings.codeFontFamily),
        ).apply {
            name = "codeFont"
            toolTipText = FONT_FALLBACK_TOOLTIP
            addActionListener { refreshPreview() }
        }
        fontScaleField = ComboBox(FONT_SCALE_CHOICES.toTypedArray()).apply {
            name = "fontScale"
            renderer = SimpleListCellRenderer.create { label, value, _ ->
                label.text = "$value%"
            }
            addActionListener { refreshPreview() }
        }
        contentWidthField = ComboBox(CONTENT_WIDTH_CHOICES.toTypedArray()).apply {
            name = "contentWidth"
            renderer = SimpleListCellRenderer.create { label, value, _ ->
                label.text = if (value == FULL_WIDTH_CHOICE) "Full width" else "$value px"
            }
            addActionListener { refreshPreview() }
        }
        val highlightGroupsRow = JPanel(FlowLayout(FlowLayout.LEADING, 12, 0)).apply {
            add(requireNotNull(accentHeadingsField))
            add(requireNotNull(accentBoldField))
            add(requireNotNull(accentInlineCodeField))
        }
        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Theme:"), requireNotNull(themeField), 1, false)
            .addLabeledComponent(JBLabel("Density:"), requireNotNull(profileField), 1, false)
            .addLabeledComponent(JBLabel("Highlight groups:"), highlightGroupsRow, 1, false)
            .addLabeledComponent(JBLabel("Body font:"), requireNotNull(bodyFontField), 1, false)
            .addLabeledComponent(JBLabel("Code font:"), requireNotNull(codeFontField), 1, false)
            .addLabeledComponent(JBLabel("Font size:"), requireNotNull(fontScaleField), 1, false)
            .addLabeledComponent(JBLabel("Maximum content width:"), requireNotNull(contentWidthField), 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
            .also { reset() }

        preview = previewFactory()
        val currentPreview = preview ?: return form
        refreshPreview()
        return JBSplitter(true, 0.38f).apply {
            firstComponent = JBScrollPane(form).apply { border = null }
            secondComponent = currentPreview.component
        }
    }

    override fun isModified(): Boolean {
        val useFullWidth = selectedUseFullWidth()
        return themeField?.selectedItem != settings.theme ||
            profileField?.selectedItem != settings.profile ||
            accentHeadingsField?.isSelected != settings.accentHeadings ||
            accentBoldField?.isSelected != settings.accentBold ||
            accentInlineCodeField?.isSelected != settings.accentInlineCode ||
            selectedFont(bodyFontField) != settings.bodyFontFamily ||
            selectedFont(codeFontField) != settings.codeFontFamily ||
            fontScaleField?.selectedItem != settings.fontScale ||
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
        bodyFontField?.selectedItem = displayedFont(settings.bodyFontFamily)
        codeFontField?.selectedItem = displayedFont(settings.codeFontFamily)
        fontScaleField?.selectedItem = settings.fontScale
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
        bodyFontField = null
        codeFontField = null
        fontScaleField = null
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
        bodyFontFamily = selectedFont(bodyFontField),
        codeFontFamily = selectedFont(codeFontField),
        fontScale = (fontScaleField?.selectedItem as? Int) ?: settings.fontScale,
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
        val FONT_SCALE_CHOICES = (MdLensSettings.MIN_FONT_SCALE..MdLensSettings.MAX_FONT_SCALE step 10).toList()
        val CONTENT_WIDTH_CHOICES: List<Int> =
            (MdLensSettings.MIN_CONTENT_WIDTH..MdLensSettings.MAX_CONTENT_WIDTH step 64).toList() + FULL_WIDTH_CHOICE
        val RECOMMENDED_BODY_FONTS = listOf(
            "Pretendard",
            "Inter",
            "Atkinson Hyperlegible",
            "Noto Serif",
            "D2Coding",
        )
        val RECOMMENDED_CODE_FONTS = listOf(
            "JetBrains Mono",
            "D2Coding",
            "Fira Code",
            "Cascadia Code",
            "Menlo",
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
