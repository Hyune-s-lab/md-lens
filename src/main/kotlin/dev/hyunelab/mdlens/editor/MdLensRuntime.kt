package dev.hyunelab.mdlens.editor

internal fun mdLensRuntimeScript(runtimeName: String): String {
    if (!RUNTIME_NAME.matches(runtimeName)) {
        return runtimeFailureScript(runtimeName, "Unsupported runtime name")
    }
    val runtime = runCatching {
        checkNotNull(
            MdLensRuntimeAnchor::class.java.getResource("/mdlens/runtime-$runtimeName.js"),
        ) { "Missing bundled runtime: $runtimeName" }.readText()
    }.getOrElse { error ->
        return runtimeFailureScript(runtimeName, error.message ?: "Missing bundled runtime")
    }
    return """
        try {
          $runtime
          window.mdLens.runtimeReady(${runtimeName.toJsonString()});
        } catch (error) {
          window.mdLens.runtimeFailed(
            ${runtimeName.toJsonString()},
            String(error && error.message ? error.message : error)
          );
        }
    """.trimIndent()
}

private fun runtimeFailureScript(runtimeName: String, message: String): String =
    "window.mdLens.runtimeFailed(${runtimeName.toJsonString()}, ${message.toJsonString()});"

private object MdLensRuntimeAnchor

private val RUNTIME_NAME = Regex("[a-z0-9-]+")
