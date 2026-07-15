package dev.hyunelab.mdlens.settings

import com.intellij.util.messages.Topic

fun interface MdLensSettingsListener {
    fun settingsChanged()

    companion object {
        @field:Topic.AppLevel
        val TOPIC: Topic<MdLensSettingsListener> = Topic.create(
            "MdLens settings changed",
            MdLensSettingsListener::class.java,
        )
    }
}
