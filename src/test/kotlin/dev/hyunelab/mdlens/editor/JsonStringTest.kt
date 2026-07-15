package dev.hyunelab.mdlens.editor

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonStringTest {
    @Test
    fun `encodes content for the renderer bridge`() {
        assertEquals(
            "\"heading\\n\\\"quoted\\\" \\\\ path\"",
            "heading\n\"quoted\" \\ path".toJsonString(),
        )
    }
}
