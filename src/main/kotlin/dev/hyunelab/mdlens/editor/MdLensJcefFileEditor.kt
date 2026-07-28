package dev.hyunelab.mdlens.editor

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import dev.hyunelab.mdlens.settings.MdLensSettings
import dev.hyunelab.mdlens.settings.MdLensSettingsListener
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.beans.PropertyChangeListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer

internal class MdLensJcefFileEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {
    private val document = requireNotNull(FileDocumentManager.getInstance().getDocument(file))
    private val pageUrl = viewerPageUrl(file)
    private val browser = JBCefBrowser()
    private val readyQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val loadRuntimeQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val openLinkQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val renderedQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val errorQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val container = JPanel(BorderLayout())
    private var focusTarget: JComponent
    private var rendererReady = false
    private var pageLoaded = false
    private var pendingAnchor: String? = null
    private var fallbackShown = false
    private var fallbackReason: String? = null
    private val errorMessages = mutableListOf<String>()
    private val consoleMessages = mutableListOf<String>()
    private val bootstrapTimer = Timer(BOOTSTRAP_TIMEOUT_MS) {
        fallBackToPlainText("Renderer did not become ready within ${BOOTSTRAP_TIMEOUT_MS / 1000}s")
    }.apply { isRepeats = false }
    @Volatile
    private var disposed = false

    init {
        Disposer.register(this, browser)
        Disposer.register(this, readyQuery)
        Disposer.register(this, loadRuntimeQuery)
        Disposer.register(this, openLinkQuery)
        Disposer.register(this, renderedQuery)
        Disposer.register(this, errorQuery)

        readyQuery.addHandler {
            LOG.debug("Renderer ready for ${file.path}")
            ApplicationManager.getApplication().invokeLater {
                if (isValid) {
                    rendererReady = true
                    bootstrapTimer.stop()
                    render()
                }
            }
            JBCefJSQuery.Response(null)
        }
        loadRuntimeQuery.addHandler { runtimeName ->
            LOG.debug("Runtime load requested: $runtimeName for ${file.path}")
            loadRuntime(runtimeName)
            JBCefJSQuery.Response(null)
        }
        openLinkQuery.addHandler { href ->
            ApplicationManager.getApplication().invokeLater {
                if (isValid) {
                    openLink(href)
                }
            }
            JBCefJSQuery.Response(null)
        }
        renderedQuery.addHandler {
            LOG.debug("Rendered callback received for ${file.path}")
            ApplicationManager.getApplication().invokeLater {
                if (isValid) {
                    applyPendingAnchor()
                }
            }
            JBCefJSQuery.Response(null)
        }
        errorQuery.addHandler { message ->
            LOG.warn("Renderer error for ${file.path}: $message")
            errorMessages.add(message)
            ApplicationManager.getApplication().invokeLater({
                if (!rendererReady) {
                    fallBackToPlainText("Renderer reported an error before becoming ready: $message")
                }
            }, ModalityState.any())
            JBCefJSQuery.Response(null)
        }

        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                render()
            }
        }, this)

        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            MdLensSettingsListener.TOPIC,
            MdLensSettingsListener { scheduleSettingsRender() },
        )

        // The Sync with IDE theme resolves at render time, so a LaF switch must re-render.
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
            LafManagerListener.TOPIC,
            LafManagerListener { scheduleSettingsRender() },
        )

        browser.jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onConsoleMessage(
                browser: CefBrowser,
                level: CefSettings.LogSeverity,
                message: String,
                source: String,
                line: Int,
            ): Boolean {
                val entry = "[$level] $message ($source:$line)"
                consoleMessages.add(entry)
                if (level == CefSettings.LogSeverity.LOGSEVERITY_ERROR) {
                    LOG.warn("JCEF console error for ${file.path}: $entry")
                }
                return true
            }
        }, browser.cefBrowser)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain && isValid) {
                    pageLoaded = true
                    LOG.debug("Viewer page loaded, connecting renderer for ${file.path}")
                    connectRenderer()
                }
            }

            override fun onLoadError(
                browser: CefBrowser,
                frame: CefFrame,
                errorCode: org.cef.handler.CefLoadHandler.ErrorCode,
                errorText: String,
                failedUrl: String,
            ) {
                if (frame.isMain && isValid) {
                    val reason = "Page load failed: $errorCode — $errorText"
                    LOG.warn("Viewer page load error for ${file.path}: $reason")
                    errorMessages.add(reason)
                    ApplicationManager.getApplication().invokeLater({
                        fallBackToPlainText(reason)
                    }, ModalityState.any())
                }
            }
        }, browser.cefBrowser)

        val viewerHtml = checkNotNull(javaClass.getResource("/mdlens/viewer.html")) {
            "Missing bundled renderer"
        }.readText()
        container.add(browser.component, BorderLayout.CENTER)
        focusTarget = browser.component
        browser.loadHTML(viewerHtml, pageUrl)
        bootstrapTimer.start()
        LOG.debug("MdLens editor created for ${file.path}")
    }

    override fun getComponent(): JComponent = container
    override fun getPreferredFocusedComponent(): JComponent = focusTarget
    override fun getName(): String = "MdLens"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) = Unit
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = !disposed && file.isValid && !project.isDisposed
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun dispose() {
        disposed = true
        rendererReady = false
        bootstrapTimer.stop()
    }

    private fun fallBackToPlainText(reason: String) {
        if (disposed || fallbackShown || rendererReady || !isValid) {
            return
        }
        fallbackShown = true
        fallbackReason = reason
        bootstrapTimer.stop()
        LOG.warn("Viewer bootstrap failed for ${file.path}; showing plain text. $reason")
        val textArea = JBTextArea(document.text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                textArea.text = event.document.text
            }
        }, this)
        val banner = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(6, 10)
        }
        banner.add(
            JBLabel("MdLens viewer failed to start; showing plain text."),
            BorderLayout.WEST,
        )
        banner.add(
            JButton("Copy Diagnostics").apply {
                addActionListener {
                    val report = buildDiagnosticReport()
                    CopyPasteManager.getInstance().setContents(StringSelection(report))
                }
            },
            BorderLayout.EAST,
        )
        container.removeAll()
        container.add(banner, BorderLayout.NORTH)
        container.add(JBScrollPane(textArea), BorderLayout.CENTER)
        focusTarget = textArea
        container.revalidate()
        container.repaint()
    }

    internal fun buildDiagnosticReport(): String {
        val settings = MdLensSettings.getInstance()
        val documentType = if (file.extension?.lowercase() in MERMAID_EXTENSIONS) "mermaid" else "markdown"
        return dev.hyunelab.mdlens.editor.buildDiagnosticReport(
            DiagnosticInfo(
                filePath = file.path,
                documentType = documentType,
                documentLength = document.text.length,
                rendererReady = rendererReady,
                pageLoaded = pageLoaded,
                fallbackReason = fallbackReason,
                errors = errorMessages.toList(),
                consoleMessages = consoleMessages.toList(),
                settings = settings,
                pluginVersion = pluginVersion(),
                ideInfo = ideInfo(),
                jcefSupported = JBCefApp.isSupported(),
                javaVersion = System.getProperty("java.version", "unknown"),
            ),
        )
    }

    private fun connectRenderer() {
        val script = """
            try {
              if (!window.mdLens) {
                throw new Error('window.mdLens is not defined — viewer script may not have loaded');
              }
              window.mdLens.connect({
                ready: function() { ${readyQuery.inject("'ready'")} },
                loadRuntime: function(name) { ${loadRuntimeQuery.inject("name")} },
                openLink: function(href) { ${openLinkQuery.inject("href")} },
                rendered: function() { ${renderedQuery.inject("'rendered'")} },
                error: function(message) { ${errorQuery.inject("message")} }
              });
            } catch (e) {
              ${errorQuery.inject("e instanceof Error ? e.message : String(e)")}
            }
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(script, pageUrl, 0)
    }

    private fun render() {
        if (!rendererReady || !isValid) {
            return
        }
        val settings = MdLensSettings.getInstance()
        val documentType = if (file.extension?.lowercase() in MERMAID_EXTENSIONS) "mermaid" else "markdown"
        val request = rendererRequestJson(document.text, pageUrl, documentType, settings)
        browser.cefBrowser.executeJavaScript("window.mdLens.render($request);", pageUrl, 0)
    }

    private fun loadRuntime(runtimeName: String) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val script = mdLensRuntimeScript(runtimeName)
            ApplicationManager.getApplication().invokeLater({
                if (isValid) {
                    browser.cefBrowser.executeJavaScript(script, pageUrl, 0)
                }
            }, ModalityState.any())
        }
    }

    private fun scheduleSettingsRender() {
        ApplicationManager.getApplication().invokeLater({
            if (isValid) {
                render()
                browser.component.repaint()
            }
        }, ModalityState.any())
    }

    private fun openLink(href: String) {
        when (val target = linkTarget(href)) {
            is LinkTarget.External -> BrowserUtil.browse(target.uri)
            is LinkTarget.LocalFile -> {
                val targetFile = VirtualFileManager.getInstance().findFileByUrl(target.vfsUrl) ?: return
                OpenFileDescriptor(project, targetFile).navigate(true)
                if (target.anchor != null) {
                    FileEditorManager.getInstance(project).getEditors(targetFile)
                        .map { editor -> if (editor is TextEditorWithPreview) editor.previewEditor else editor }
                        .filterIsInstance<MdLensJcefFileEditor>()
                        .firstOrNull()
                        ?.scrollToAnchor(target.anchor)
                }
            }
            null -> Unit
        }
    }

    private fun scrollToAnchor(anchor: String) {
        pendingAnchor = anchor
        if (rendererReady) {
            applyPendingAnchor()
        }
    }

    private fun applyPendingAnchor() {
        val anchor = pendingAnchor ?: return
        pendingAnchor = null
        browser.cefBrowser.executeJavaScript(
            "document.getElementById(${anchor.toJsonString()})?.scrollIntoView();",
            pageUrl,
            0,
        )
    }

    private companion object {
        val LOG = Logger.getInstance(MdLensJcefFileEditor::class.java)
        val MERMAID_EXTENSIONS = setOf("mermaid", "mmd")
        const val BOOTSTRAP_TIMEOUT_MS = 30_000

        private fun pluginVersion(): String =
            javaClass.getResourceAsStream("/mdlens/plugin-version.txt")?.bufferedReader()?.use { it.readText().trim() } ?: "unknown"

        private fun ideInfo(): String {
            val info = ApplicationInfo.getInstance()
            return "${info.fullApplicationName} (build ${info.build})"
        }
    }
}
