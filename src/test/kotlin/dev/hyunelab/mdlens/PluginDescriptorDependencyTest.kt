package dev.hyunelab.mdlens

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PluginDescriptorDependencyTest {
    @Test
    fun `depends only on the platform module and probes JCEF at runtime`() {
        val descriptor = requireNotNull(javaClass.getResource("/META-INF/plugin.xml")).readText()

        assertTrue(
            "<depends>com.intellij.modules.platform</depends>" in descriptor,
            "The plugin must declare the platform module dependency",
        )
        // JCEF availability is probed at runtime via JBCefApp.isSupported(); an optional
        // module dependency is unnecessary and fails to resolve on 2025.1 in the plugin verifier.
        assertFalse(
            "com.intellij.modules.jcef" in descriptor,
            "The plugin must not declare a JCEF module dependency",
        )
        assertNull(javaClass.getResource("/META-INF/jcef.xml"))
    }
}
