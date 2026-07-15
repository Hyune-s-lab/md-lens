package dev.hyunelab.mdlens.editor

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ViewerPageUrlVirtualFileTest : BasePlatformTestCase() {
    fun testEncodesLocalFileUrls() {
        val ioFile = FileUtil.createTempFile("마크다운 테스트", ".md")
        try {
            val file = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile))
            val url = viewerPageUrl(file)
            assertTrue(url.startsWith("file:///"))
            assertTrue(url.contains("%EB%A7%88%ED%81%AC%EB%8B%A4%EC%9A%B4%20%ED%85%8C%EC%8A%A4%ED%8A%B8"))
            assertFalse(url.contains("마크다운"))
        } finally {
            FileUtil.delete(ioFile)
        }
    }

    fun testFallsBackToVirtualFileUrlWithoutNioPath() {
        val file = LightVirtualFile("README.md", PlainTextLanguage.INSTANCE, "# MdLens")
        assertEquals(file.url, viewerPageUrl(file))
    }
}
