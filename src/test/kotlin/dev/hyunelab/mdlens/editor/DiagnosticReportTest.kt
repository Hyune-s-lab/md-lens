package dev.hyunelab.mdlens.editor

import dev.hyunelab.mdlens.settings.MdLensProfile
import dev.hyunelab.mdlens.settings.MdLensSettings
import dev.hyunelab.mdlens.settings.MdLensTheme
import kotlin.test.Test
import kotlin.test.assertTrue

class DiagnosticReportTest {

    private fun diagnosticInfo(
        filePath: String = "/path/to/file.md",
        documentType: String = "markdown",
        documentLength: Int = 1234,
        rendererReady: Boolean = false,
        fallbackReason: String? = "Renderer did not become ready within 30s",
        errors: List<String> = listOf("Renderer error for /path/to/file.md: boom"),
        settings: MdLensSettings = MdLensSettings().apply {
            updateAppearance(
                theme = MdLensTheme.DARK,
                profile = MdLensProfile.SPACIOUS,
                fontFamily = "Inter",
                fontSize = 16,
                maxContentWidth = 1280,
                useFullWidth = false,
            )
        },
    ) = DiagnosticInfo(
        filePath = filePath,
        documentType = documentType,
        documentLength = documentLength,
        rendererReady = rendererReady,
        pageLoaded = false,
        pageReloadAttempts = 0,
        fallbackReason = fallbackReason,
        errors = errors,
        consoleMessages = emptyList(),
        settings = settings,
        pluginVersion = "0.5.1",
        ideInfo = "IntelliJ IDEA 2025.1 (build 251.12345)",
        jcefSupported = true,
        javaVersion = "21.0.5",
    )

    @Test
    fun `includes file and renderer info in the report`() {
        val report = buildDiagnosticReport(diagnosticInfo())

        assertTrue(report.contains("## MdLens Diagnostic Report"), "must have title")
        assertTrue(report.contains("**MdLens version**: 0.5.1"), "must have plugin version")
        assertTrue(report.contains("**IDE**:"), "must have IDE info")
        assertTrue(report.contains("**JCEF**: Supported"), "must show JCEF supported")
        assertTrue(report.contains("**Java**: 21.0.5"), "must show Java version")
        assertTrue(report.contains("/path/to/file.md"), "must have file path")
        assertTrue(report.contains("1234 chars"), "must have document length")
        assertTrue(report.contains("not ready"), "must show renderer state")
        assertTrue(report.contains("Renderer did not become ready within 30s"), "must show fallback reason")
        assertTrue(report.contains("Theme: GitHub Dark"), "must show theme")
        assertTrue(report.contains("Profile: Spacious"), "must show profile")
        assertTrue(report.contains("Inter 16px"), "must show font")
        assertTrue(report.contains("1280px"), "must show content width")
        assertTrue(report.contains("Renderer error for /path/to/file.md: boom"), "must include errors")
    }

    @Test
    fun `omits errors section when there are no errors`() {
        val info = diagnosticInfo(
            filePath = "/empty.md",
            documentLength = 0,
            rendererReady = true,
            fallbackReason = null,
            errors = emptyList(),
        )

        val report = buildDiagnosticReport(info)

        assertTrue(report.contains("ready"), "must show ready state")
        assertTrue(!report.contains("### Errors"), "must not have errors section when empty")
    }
}
