package dev.hyunelab.mdlens.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag

enum class MdLensTheme(
    val displayName: String,
    val wireValue: String,
) {
    LIGHT("GitHub Light", "light"),
    DARK("GitHub Dark", "dark"),
    ;

    override fun toString(): String = displayName
}

enum class MdLensProfile(
    val displayName: String,
    val wireValue: String,
) {
    COMPACT("Compact", "compact"),
    SPACIOUS("Spacious", "spacious"),
    ;

    override fun toString(): String = displayName
}

data class MdLensAppearance(
    val theme: MdLensTheme,
    val profile: MdLensProfile,
    val fontFamily: String,
    val fontSize: Int,
    val maxContentWidth: Int,
    val useFullWidth: Boolean,
    val accentHeadings: Boolean = false,
    val accentBold: Boolean = false,
    val accentInlineCode: Boolean = false,
)

@State(
    name = "dev.hyunelab.mdlens.settings.MdLensSettings",
    storages = [Storage("mdlens.xml")],
)
class MdLensSettings : PersistentStateComponent<MdLensSettings.SettingsState> {
    data class SettingsState(
        @OptionTag(converter = MdLensThemeConverter::class)
        var theme: MdLensTheme = MdLensTheme.LIGHT,
        @OptionTag(converter = MdLensProfileConverter::class)
        var profile: MdLensProfile = MdLensProfile.COMPACT,
        var fontFamily: String = "",
        var fontSize: Int = DEFAULT_FONT_SIZE,
        var maxContentWidth: Int = DEFAULT_CONTENT_WIDTH,
        var useFullWidth: Boolean = true,
        var accentHeadings: Boolean = false,
        var accentBold: Boolean = false,
        var accentInlineCode: Boolean = false,
        var accentsInitialized: Boolean = false,
    )

    private var settingsState = normalizedState(SettingsState())

    val theme: MdLensTheme
        get() = settingsState.theme

    val profile: MdLensProfile
        get() = settingsState.profile

    val fontFamily: String
        get() = settingsState.fontFamily

    val fontSize: Int
        get() = settingsState.fontSize

    val maxContentWidth: Int
        get() = settingsState.maxContentWidth

    val useFullWidth: Boolean
        get() = settingsState.useFullWidth

    val accentHeadings: Boolean
        get() = settingsState.accentHeadings

    val accentBold: Boolean
        get() = settingsState.accentBold

    val accentInlineCode: Boolean
        get() = settingsState.accentInlineCode

    val appearance: MdLensAppearance
        get() = MdLensAppearance(
            theme,
            profile,
            fontFamily,
            fontSize,
            maxContentWidth,
            useFullWidth,
            accentHeadings,
            accentBold,
            accentInlineCode,
        )

    fun updateAppearance(appearance: MdLensAppearance): Boolean = updateAppearance(
        theme = appearance.theme,
        profile = appearance.profile,
        fontFamily = appearance.fontFamily,
        fontSize = appearance.fontSize,
        maxContentWidth = appearance.maxContentWidth,
        useFullWidth = appearance.useFullWidth,
        accentHeadings = appearance.accentHeadings,
        accentBold = appearance.accentBold,
        accentInlineCode = appearance.accentInlineCode,
    )

    fun updateAppearance(
        theme: MdLensTheme,
        profile: MdLensProfile,
        fontFamily: String,
        fontSize: Int,
        maxContentWidth: Int,
        useFullWidth: Boolean,
        accentHeadings: Boolean = false,
        accentBold: Boolean = false,
        accentInlineCode: Boolean = false,
    ): Boolean {
        val nextState = normalizedState(
            SettingsState(
                theme,
                profile,
                fontFamily,
                fontSize,
                maxContentWidth,
                useFullWidth,
                accentHeadings,
                accentBold,
                accentInlineCode,
                accentsInitialized = true,
            ),
        )
        if (settingsState == nextState) {
            return false
        }
        settingsState = nextState
        return true
    }

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = normalizedState(state)
    }

    companion object {
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 24
        const val DEFAULT_FONT_SIZE = 14
        const val MIN_CONTENT_WIDTH = 768
        const val MAX_CONTENT_WIDTH = 1536
        const val DEFAULT_CONTENT_WIDTH = 1152

        fun getInstance(): MdLensSettings =
            ApplicationManager.getApplication().getService(MdLensSettings::class.java)

        private fun normalizedState(state: SettingsState): SettingsState = state.copy(
            fontFamily = state.fontFamily.trim(),
            fontSize = state.fontSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
            maxContentWidth = state.maxContentWidth.coerceIn(MIN_CONTENT_WIDTH, MAX_CONTENT_WIDTH),
            accentHeadings = if (state.accentsInitialized) {
                state.accentHeadings
            } else {
                state.profile == MdLensProfile.SPACIOUS
            },
            accentBold = state.accentsInitialized && state.accentBold,
            accentInlineCode = state.accentsInitialized && state.accentInlineCode,
            accentsInitialized = true,
        )
    }
}

internal class MdLensThemeConverter : Converter<MdLensTheme>() {
    override fun fromString(value: String): MdLensTheme = when (value.lowercase()) {
        "dark", "github dark" -> MdLensTheme.DARK
        else -> MdLensTheme.LIGHT
    }

    override fun toString(value: MdLensTheme): String = value.wireValue
}

internal class MdLensProfileConverter : Converter<MdLensProfile>() {
    override fun fromString(value: String): MdLensProfile = when (value.lowercase()) {
        "spacious" -> MdLensProfile.SPACIOUS
        else -> MdLensProfile.COMPACT
    }

    override fun toString(value: MdLensProfile): String = value.wireValue
}
