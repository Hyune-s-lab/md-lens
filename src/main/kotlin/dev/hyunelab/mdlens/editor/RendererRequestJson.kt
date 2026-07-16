package dev.hyunelab.mdlens.editor

import dev.hyunelab.mdlens.settings.MdLensSettings

internal fun rendererRequestJson(
    source: String,
    baseUrl: String,
    documentType: String,
    settings: MdLensSettings,
): String =
    """{"version":5,"source":${source.toJsonString()},"baseUrl":${baseUrl.toJsonString()},"documentType":"$documentType","theme":"${settings.theme.resolveForRendering().wireValue}","profile":"${settings.profile.wireValue}","fontFamily":${settings.fontFamily.toJsonString()},"fontSize":${settings.fontSize},"maxContentWidth":${if (settings.useFullWidth) "null" else settings.maxContentWidth},"accentHeadings":${settings.accentHeadings},"accentBold":${settings.accentBold},"accentInlineCode":${settings.accentInlineCode},"accentHeadingsColor":"${settings.accentHeadingsColor.wireValue}","accentBoldColor":"${settings.accentBoldColor.wireValue}","accentInlineCodeColor":"${settings.accentInlineCodeColor.wireValue}"}"""
