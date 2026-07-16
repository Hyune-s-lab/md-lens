package dev.hyunelab.mdlens.editor

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManagerListener
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import dev.hyunelab.mdlens.settings.MdLensSettings
import dev.hyunelab.mdlens.settings.MdLensSettingsListener
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.beans.PropertyChangeListener
import javax.swing.JComponent

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
    private var rendererReady = false
    private var pendingAnchor: String? = null
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
            ApplicationManager.getApplication().invokeLater {
                if (isValid) {
                    rendererReady = true
                    render()
                }
            }
            JBCefJSQuery.Response(null)
        }
        loadRuntimeQuery.addHandler { runtimeName ->
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
            ApplicationManager.getApplication().invokeLater {
                if (isValid) {
                    applyPendingAnchor()
                }
            }
            JBCefJSQuery.Response(null)
        }
        errorQuery.addHandler { message ->
            LOG.warn("Renderer error for ${file.path}: $message")
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

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain && isValid) {
                    connectRenderer()
                }
            }
        }, browser.cefBrowser)

        val viewerHtml = checkNotNull(javaClass.getResource("/mdlens/viewer.html")) {
            "Missing bundled renderer"
        }.readText()
        browser.loadHTML(viewerHtml, pageUrl)
    }

    override fun getComponent(): JComponent = browser.component
    override fun getPreferredFocusedComponent(): JComponent = browser.component
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
    }

    private fun connectRenderer() {
        val script = """
            window.mdLens.connect({
              ready: function() { ${readyQuery.inject("'ready'")} },
              loadRuntime: function(name) { ${loadRuntimeQuery.inject("name")} },
              openLink: function(href) { ${openLinkQuery.inject("href")} },
              rendered: function() { ${renderedQuery.inject("'rendered'")} },
              error: function(message) { ${errorQuery.inject("message")} }
            });
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
    }
}
