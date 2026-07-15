package dev.hyunelab.mdlens.editor

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAware
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MdLensFileEditorProviderTest : BasePlatformTestCase() {
    fun testProviderContract() {
        val provider = MdLensFileEditorProvider()

        assertTrue(provider.accept(project, LightVirtualFile("README.md", PlainTextLanguage.INSTANCE, "# MdLens")))
        assertTrue(provider.accept(project, LightVirtualFile("guide.markdown", PlainTextLanguage.INSTANCE, "# Guide")))
        assertTrue(provider.accept(project, LightVirtualFile("system.mmd", PlainTextLanguage.INSTANCE, "flowchart LR")))
        assertTrue(provider.accept(project, LightVirtualFile("system.mermaid", PlainTextLanguage.INSTANCE, "flowchart LR")))
        assertFalse(provider.accept(project, LightVirtualFile("notes.txt", PlainTextLanguage.INSTANCE, "notes")))
        assertEquals(FileEditorPolicy.HIDE_OTHER_EDITORS, provider.policy)
        assertTrue((provider as Any) is DumbAware)
    }
}
