package dev.hyunelab.mdlens.settings

import javax.swing.JComponent

internal interface MdLensSettingsPreview {
    val component: JComponent

    fun render(appearance: MdLensAppearance)

    fun dispose()
}
