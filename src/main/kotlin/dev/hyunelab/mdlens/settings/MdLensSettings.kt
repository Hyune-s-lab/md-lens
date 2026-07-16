package dev.hyunelab.mdlens.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.ui.JBColor
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag

enum class MdLensTheme(
    val displayName: String,
    val wireValue: String,
) {
    SYNC("Sync with IDE", "sync"),
    LIGHT("GitHub Light", "light"),
    DARK("GitHub Dark", "dark"),
    ;

    override fun toString(): String = displayName

    /** The renderer bridge only understands light/dark; SYNC resolves against the IDE theme. */
    fun resolveForRendering(): MdLensTheme = when (this) {
        SYNC -> if (runCatching { !JBColor.isBright() }.getOrDefault(false)) DARK else LIGHT
        else -> this
    }
}

enum class MdLensAccentColor(
    val displayName: String,
    val wireValue: String,
    val lightHex: String,
    val darkHex: String,
) {
    ORANGE("Orange", "orange", "#bc4c00", "#f0883e"),
    GOLD("Gold", "gold", "#9a6700", "#e3b341"),
    GREEN("Green", "green", "#116329", "#7ee787"),
    TEAL("Teal", "teal", "#1b7c83", "#76e3ea"),
    BLUE("Blue", "blue", "#0969da", "#79c0ff"),
    PURPLE("Purple", "purple", "#8250df", "#d2a8ff"),
    PINK("Pink", "pink", "#bf3989", "#f778ba"),
    RED("Red", "red", "#cf222e", "#ff7b72"),
    ;

    override fun toString(): String = displayName

    companion object {
        fun fromWireValue(value: String): MdLensAccentColor? =
            entries.firstOrNull { it.wireValue == value.lowercase() }
    }
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
    val accentHeadingsColor: MdLensAccentColor = MdLensAccentColor.ORANGE,
    val accentBoldColor: MdLensAccentColor = MdLensAccentColor.GOLD,
    val accentInlineCodeColor: MdLensAccentColor = MdLensAccentColor.GREEN,
)

@State(
    name = "dev.hyunelab.mdlens.settings.MdLensSettings",
    storages = [Storage("mdlens.xml")],
)
class MdLensSettings : PersistentStateComponent<MdLensSettings.SettingsState> {
    data class SettingsState(
        @OptionTag(converter = MdLensThemeConverter::class)
        var theme: MdLensTheme = MdLensTheme.SYNC,
        @OptionTag(converter = MdLensProfileConverter::class)
        var profile: MdLensProfile = MdLensProfile.SPACIOUS,
        var fontFamily: String = "",
        var fontSize: Int = DEFAULT_FONT_SIZE,
        var maxContentWidth: Int = DEFAULT_CONTENT_WIDTH,
        var useFullWidth: Boolean = true,
        var accentHeadings: Boolean = false,
        var accentBold: Boolean = false,
        var accentInlineCode: Boolean = false,
        var accentsInitialized: Boolean = false,
        @OptionTag(converter = MdLensHeadingsAccentColorConverter::class)
        var accentHeadingsColor: MdLensAccentColor = MdLensAccentColor.ORANGE,
        @OptionTag(converter = MdLensBoldAccentColorConverter::class)
        var accentBoldColor: MdLensAccentColor = MdLensAccentColor.GOLD,
        @OptionTag(converter = MdLensInlineCodeAccentColorConverter::class)
        var accentInlineCodeColor: MdLensAccentColor = MdLensAccentColor.GREEN,
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

    val accentHeadingsColor: MdLensAccentColor
        get() = settingsState.accentHeadingsColor

    val accentBoldColor: MdLensAccentColor
        get() = settingsState.accentBoldColor

    val accentInlineCodeColor: MdLensAccentColor
        get() = settingsState.accentInlineCodeColor

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
            accentHeadingsColor,
            accentBoldColor,
            accentInlineCodeColor,
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
        accentHeadingsColor = appearance.accentHeadingsColor,
        accentBoldColor = appearance.accentBoldColor,
        accentInlineCodeColor = appearance.accentInlineCodeColor,
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
        accentHeadingsColor: MdLensAccentColor = MdLensAccentColor.ORANGE,
        accentBoldColor: MdLensAccentColor = MdLensAccentColor.GOLD,
        accentInlineCodeColor: MdLensAccentColor = MdLensAccentColor.GREEN,
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
                accentHeadingsColor = accentHeadingsColor,
                accentBoldColor = accentBoldColor,
                accentInlineCodeColor = accentInlineCodeColor,
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
        "sync", "sync with ide" -> MdLensTheme.SYNC
        "dark", "github dark" -> MdLensTheme.DARK
        else -> MdLensTheme.LIGHT
    }

    override fun toString(value: MdLensTheme): String = value.wireValue
}

internal abstract class MdLensAccentColorConverter(
    private val fallback: MdLensAccentColor,
) : Converter<MdLensAccentColor>() {
    override fun fromString(value: String): MdLensAccentColor =
        MdLensAccentColor.fromWireValue(value) ?: fallback

    override fun toString(value: MdLensAccentColor): String = value.wireValue
}

internal class MdLensHeadingsAccentColorConverter :
    MdLensAccentColorConverter(MdLensAccentColor.ORANGE)

internal class MdLensBoldAccentColorConverter :
    MdLensAccentColorConverter(MdLensAccentColor.GOLD)

internal class MdLensInlineCodeAccentColorConverter :
    MdLensAccentColorConverter(MdLensAccentColor.GREEN)

internal class MdLensProfileConverter : Converter<MdLensProfile>() {
    override fun fromString(value: String): MdLensProfile = when (value.lowercase()) {
        "spacious" -> MdLensProfile.SPACIOUS
        else -> MdLensProfile.COMPACT
    }

    override fun toString(value: MdLensProfile): String = value.wireValue
}
