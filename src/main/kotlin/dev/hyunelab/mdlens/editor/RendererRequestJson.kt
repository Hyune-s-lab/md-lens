package dev.hyunelab.mdlens.editor

import dev.hyunelab.mdlens.settings.MdLensSettings

internal fun rendererRequestJson(
    source: String,
    baseUrl: String,
    documentType: String,
    settings: MdLensSettings,
): String =
    """{"version":4,"source":${source.toJsonString()},"baseUrl":${baseUrl.toJsonString()},"documentType":"$documentType","theme":"${settings.theme.wireValue}","profile":"${settings.profile.wireValue}","bodyFontFamily":${settings.bodyFontFamily.toJsonString()},"codeFontFamily":${settings.codeFontFamily.toJsonString()},"fontScale":${settings.fontScale},"maxContentWidth":${if (settings.useFullWidth) "null" else settings.maxContentWidth},"accentHeadings":${settings.accentHeadings},"accentBold":${settings.accentBold},"accentInlineCode":${settings.accentInlineCode}}"""
