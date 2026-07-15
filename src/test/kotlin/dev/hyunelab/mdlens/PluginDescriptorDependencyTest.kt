package dev.hyunelab.mdlens

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginDescriptorDependencyTest {
    @Test
    fun `declares the JCEF plugin required by the editor`() {
        val descriptor = requireNotNull(javaClass.getResource("/META-INF/plugin.xml")).readText()

        assertTrue(
            "<depends optional=\"true\" config-file=\"jcef.xml\">com.intellij.modules.jcef</depends>" in descriptor,
            "JCEF API classes must be visible when the IDE provides the JCEF plugin",
        )
        assertNotNull(javaClass.getResource("/META-INF/jcef.xml"))
    }
}
