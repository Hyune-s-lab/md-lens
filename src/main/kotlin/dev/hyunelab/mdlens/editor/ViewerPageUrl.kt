package dev.hyunelab.mdlens.editor

import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Path

// JCEF registers the viewer HTML under the exact URL string passed to loadHTML, but Chromium
// canonicalizes the URL (percent-encoding non-ASCII characters and spaces) before the scheme
// handler looks it up. A non-canonical URL misses the lookup and JCEF falls back to loading
// the raw file from disk, so the URL must be percent-encoded up front.
internal fun viewerPageUrl(file: VirtualFile): String =
    runCatching { viewerPageUrl(file.toNioPath()) }.getOrDefault(file.url)

internal fun viewerPageUrl(path: Path): String = path.toUri().toASCIIString()
