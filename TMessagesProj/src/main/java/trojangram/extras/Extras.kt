/*
 * TrojanGram — quality-of-life extras
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.extras

import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs

/**
 * The AyuGram/exteraGram quality-of-life toggles that are purely local: they change what YOU see
 * and how YOUR client behaves. Nothing here touches another person's data or Telegram's revenue.
 */
object Extras {

    private fun on(key: String, default: Boolean = false) =
        FeatureFlags.isEnabled(FeatureFlags.Feature.EXTRAS) && TrojanPrefs.getBoolean(key, default)

    fun showMessageId() = on(TrojanPrefs.Keys.EXTRAS_SHOW_ID)
    fun showSeconds() = on(TrojanPrefs.Keys.EXTRAS_SHOW_SECONDS)
    fun watermark() = if (on(TrojanPrefs.Keys.EXTRAS_WATERMARK, false))
        TrojanPrefs.getString(TrojanPrefs.Keys.EXTRAS_WATERMARK, "") else ""
    fun silentByDefault() = on(TrojanPrefs.Keys.EXTRAS_SILENT_BY_DEFAULT)
    fun confirmSticker() = on(TrojanPrefs.Keys.EXTRAS_CONFIRM_STICKER)
    fun confirmVoice() = on(TrojanPrefs.Keys.EXTRAS_CONFIRM_VOICE, true)
    fun keepAlive() = on(TrojanPrefs.Keys.EXTRAS_KEEP_ALIVE)

    /** "Default" | "System" | "Monospace" */
    fun font() = TrojanPrefs.getString(TrojanPrefs.Keys.EXTRAS_FONT, "Default")

    /** Adds the watermark text to an exported file name or image caption. */
    fun decorate(name: String): String {
        val wm = watermark()
        return if (wm.isBlank()) name else "$name · $wm"
    }
}
