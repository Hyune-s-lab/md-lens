package dev.hyunelab.mdlens.editor

import dev.hyunelab.mdlens.settings.MdLensSettings

internal data class DiagnosticInfo(
    val filePath: String,
    val documentType: String,
    val documentLength: Int,
    val rendererReady: Boolean,
    val pageLoaded: Boolean,
    val pageReloadAttempts: Int,
    val fallbackReason: String?,
    val errors: List<String>,
    val consoleMessages: List<String>,
    val settings: MdLensSettings,
    val pluginVersion: String,
    val ideInfo: String,
    val jcefSupported: Boolean,
    val javaVersion: String,
)

internal fun buildDiagnosticReport(info: DiagnosticInfo): String = buildString {
    appendLine("## MdLens Diagnostic Report")
    appendLine()
    appendLine("**MdLens version**: ${info.pluginVersion}")
    appendLine("**IDE**: ${info.ideInfo}")
    appendLine("**JCEF**: ${if (info.jcefSupported) "Supported" else "Not supported"}")
    appendLine("**Java**: ${info.javaVersion}")
    appendLine()
    appendLine("**File**: ${info.filePath}")
    appendLine("**Document type**: ${info.documentType}")
    appendLine("**Document length**: ${info.documentLength} chars")
    appendLine()
    val state = buildString {
        append(if (info.rendererReady) "ready" else "not ready")
        if (info.fallbackReason != null) {
            append(" — ${info.fallbackReason}")
        }
    }
    appendLine("**Renderer state**: $state")
    appendLine("**Viewer page loaded**: ${if (info.pageLoaded) "yes" else "no"}")
    if (info.pageReloadAttempts > 0) {
        appendLine("**Page reload attempts**: ${info.pageReloadAttempts}")
    }
    appendLine()
    appendLine("### Settings")
    appendLine()
    appendLine("- Theme: ${info.settings.theme}")
    appendLine("- Profile: ${info.settings.profile}")
    appendLine("- Font: ${info.settings.fontFamily.ifEmpty { "(default)" }} ${info.settings.fontSize}px")
    appendLine("- Content width: ${if (info.settings.useFullWidth) "full" else "${info.settings.maxContentWidth}px"}")
    appendLine()
    if (info.errors.isNotEmpty()) {
        appendLine("### Errors")
        appendLine()
        for (error in info.errors) {
            appendLine("- $error")
        }
        appendLine()
    }
    if (info.consoleMessages.isNotEmpty()) {
        appendLine("### Console")
        appendLine()
        for (msg in info.consoleMessages) {
            appendLine("- $msg")
        }
    }
}
