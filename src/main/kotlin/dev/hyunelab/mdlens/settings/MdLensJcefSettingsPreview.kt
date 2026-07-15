package dev.hyunelab.mdlens.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import dev.hyunelab.mdlens.editor.mdLensRuntimeScript
import dev.hyunelab.mdlens.editor.rendererRequestJson
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.JComponent

internal fun createMdLensSettingsPreview(): MdLensSettingsPreview? {
    if (!runCatching { JBCefApp.isSupported() }.getOrDefault(false)) {
        return null
    }
    return runCatching { MdLensJcefSettingsPreview() }.getOrNull()
}

internal val MARKDOWN_NEAT_SETTINGS_PREVIEW_SAMPLE = """
    # H1 — Reading Preview

    The preview uses the same renderer as a document, including **bold text**, `inline code`, and [links](#configuration-example).

    ## H2 — Typography hierarchy

    ### H3 — Text styles

    Compare headings, paragraphs, and emphasis before applying appearance changes.

    ## H2 — TypeScript example

    ```typescript
    interface RenderRequest {
      source: string;
      density: "compact" | "spacious";
    }

    export function render(request: RenderRequest): string {
      const label = request.density === "spacious" ? "Reading" : "Compact";
      return `${'$'}{label}: ${'$'}{request.source.length} characters`;
    }
    ```

    ### H3 — Configuration example

    ```yaml
    viewer:
      mode: read-only
      theme: github-dark
      density: spacious
      typography:
        scale: 110
        content-width: full
      offline: true
    ```

    > Blockquotes follow the density, and highlight groups accent headings, bold, and inline code.

    - Compact keeps familiar GitHub spacing.
    - Spacious is tuned for longer reading sessions.
""".trimIndent()

private class MdLensJcefSettingsPreview : MdLensSettingsPreview {
    private val browser = JBCefBrowser()
    private val loadRuntimeQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val previewSettings = MdLensSettings()

    @Volatile
    private var rendererReady = false

    @Volatile
    private var pendingRequest: String? = null

    @Volatile
    private var disposed = false

    override val component: JComponent = browser.component.apply { name = "appearancePreview" }

    init {
        Disposer.register(browser, loadRuntimeQuery)
        loadRuntimeQuery.addHandler { runtimeName ->
            loadRuntime(runtimeName)
            JBCefJSQuery.Response(null)
        }
        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (!frame.isMain || disposed) {
                    return
                }
                rendererReady = true
                browser.executeJavaScript(connectScript(), PREVIEW_URL, 0)
                pendingRequest?.let(::executeRender)
            }
        }, browser.cefBrowser)

        val viewerHtml = checkNotNull(javaClass.getResource("/mdlens/viewer.html")) {
            "Missing bundled renderer"
        }.readText()
        browser.loadHTML(viewerHtml, PREVIEW_URL)
    }

    override fun render(appearance: MdLensAppearance) {
        if (disposed) {
            return
        }
        previewSettings.updateAppearance(appearance)
        val request = rendererRequestJson(
            source = MARKDOWN_NEAT_SETTINGS_PREVIEW_SAMPLE,
            baseUrl = PREVIEW_URL,
            documentType = "markdown",
            settings = previewSettings,
        )
        pendingRequest = request
        if (rendererReady) {
            executeRender(request)
        }
    }

    override fun dispose() {
        disposed = true
        rendererReady = false
        pendingRequest = null
        Disposer.dispose(browser)
    }

    private fun executeRender(request: String) {
        if (disposed) {
            return
        }
        browser.cefBrowser.executeJavaScript(
            "window.mdLens.render($request);",
            PREVIEW_URL,
            0,
        )
    }

    private fun loadRuntime(runtimeName: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val script = mdLensRuntimeScript(runtimeName)
            ApplicationManager.getApplication().invokeLater({
                if (!disposed) {
                    browser.cefBrowser.executeJavaScript(script, PREVIEW_URL, 0)
                }
            }, ModalityState.any())
        }
    }

    private fun connectScript(): String = """
        window.mdLens.connect({
          ready: function() {},
          loadRuntime: function(name) { ${loadRuntimeQuery.inject("name")} },
          openLink: function() {},
          rendered: function() {},
          error: function() {}
        });
    """.trimIndent()

    private companion object {
        const val PREVIEW_URL = "file:///mdlens-preview.md"
    }
}
