package dev.hyunelab.mdlens

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginDescriptorDependencyTest {
    @Test
    fun `declares the JCEF plugin required by the editor`() {
        val descriptor = requireNotNull(javaClass.getResource("/META-INF/plugin.xml")).readText()

        // The optional dependency wires the JCEF module's class loader as a parent of the
        // plugin's class loader. Since 2026.2 the com.intellij.ui.jcef classes live in the
        // separate com.intellij.modules.jcef module, so dropping this declaration breaks the
        // plugin with ClassNotFoundException: com.intellij.ui.jcef.JBCefApp. On 2025.1 the
        // module does not resolve (plugin verifier warns), which is harmless because the JCEF
        // classes are still part of the platform there.
        assertTrue(
            "<depends optional=\"true\" config-file=\"jcef.xml\">com.intellij.modules.jcef</depends>" in descriptor,
            "JCEF API classes must be visible when the IDE provides the JCEF plugin",
        )
        assertNotNull(javaClass.getResource("/META-INF/jcef.xml"))
    }
}
