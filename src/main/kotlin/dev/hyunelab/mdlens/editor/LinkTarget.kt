package dev.hyunelab.mdlens.editor

import java.net.URI

internal sealed interface LinkTarget {
    data class External(val uri: URI) : LinkTarget

    data class LocalFile(val vfsUrl: String, val anchor: String?) : LinkTarget
}

internal fun linkTarget(href: String): LinkTarget? {
    val uri = runCatching { URI(href) }.getOrNull() ?: return null
    return when (uri.scheme?.lowercase()) {
        "http", "https", "mailto" -> LinkTarget.External(uri)
        "file" -> {
            val path = uri.path?.takeUnless { it.isEmpty() } ?: return null
            LinkTarget.LocalFile("file://$path", uri.fragment?.takeUnless { it.isEmpty() })
        }
        else -> null
    }
}
