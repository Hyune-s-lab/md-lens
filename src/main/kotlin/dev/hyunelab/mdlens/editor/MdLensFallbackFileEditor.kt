package dev.hyunelab.mdlens.editor

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.beans.PropertyChangeListener
import javax.swing.JComponent

internal class MdLensFallbackFileEditor(file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val document = requireNotNull(FileDocumentManager.getInstance().getDocument(file))
    private val textArea = JBTextArea(document.text).apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }
    private val component = JBScrollPane(textArea)

    init {
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                textArea.text = event.document.text
            }
        }, this)
    }

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent = textArea
    override fun getName(): String = "MdLens"
    override fun setState(state: FileEditorState) = Unit
    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE
    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = true
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit
    override fun dispose() = Unit
}
