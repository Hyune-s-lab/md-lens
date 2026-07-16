package dev.hyunelab.mdlens.editor

import dev.hyunelab.mdlens.settings.MdLensAccentColor
import dev.hyunelab.mdlens.settings.MdLensProfile
import dev.hyunelab.mdlens.settings.MdLensSettings
import dev.hyunelab.mdlens.settings.MdLensTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RendererRequestJsonTest {
    @Test
    fun `serializes the complete appearance contract safely`() {
        val settings = MdLensSettings().apply {
            updateAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                fontFamily = "Font \"One\"",
                fontSize = 16,
                maxContentWidth = 1280,
                useFullWidth = true,
                accentHeadings = true,
            )
        }

        val request = rendererRequestJson(
            source = "# Read\n",
            baseUrl = "file:///README.md",
            documentType = "markdown",
            settings = settings,
        )

        assertEquals(
            """{"version":5,"source":"# Read\n","baseUrl":"file:///README.md","documentType":"markdown","theme":"dark","profile":"spacious","fontFamily":"Font \"One\"","fontSize":16,"maxContentWidth":null,"accentHeadings":true,"accentBold":false,"accentInlineCode":false,"accentHeadingsColor":"orange","accentBoldColor":"gold","accentInlineCodeColor":"green"}""",
            request,
        )

        settings.updateAppearance(
            settings.appearance.copy(
                useFullWidth = false,
                accentInlineCode = true,
                accentBoldColor = MdLensAccentColor.PINK,
            ),
        )
        assertEquals(
            """{"version":5,"source":"# Read\n","baseUrl":"file:///README.md","documentType":"markdown","theme":"dark","profile":"spacious","fontFamily":"Font \"One\"","fontSize":16,"maxContentWidth":1280,"accentHeadings":true,"accentBold":false,"accentInlineCode":true,"accentHeadingsColor":"orange","accentBoldColor":"pink","accentInlineCodeColor":"green"}""",
            rendererRequestJson(
                source = "# Read\n",
                baseUrl = "file:///README.md",
                documentType = "markdown",
                settings = settings,
            ),
        )
    }

    @Test
    fun `resolves the sync theme to a concrete wire theme`() {
        val settings = MdLensSettings()
        assertEquals(MdLensTheme.SYNC, settings.theme)

        val request = rendererRequestJson(
            source = "# Read\n",
            baseUrl = "file:///README.md",
            documentType = "markdown",
            settings = settings,
        )

        assertTrue(
            """"theme":"light"""" in request || """"theme":"dark"""" in request,
            "sync must never reach the renderer: $request",
        )
    }
}
