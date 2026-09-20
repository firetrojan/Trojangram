/*
 * TrojanGram — ghost mode gate
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.spy

import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs

/**
 * Decides which presence signals leave the device.
 *
 * "Seen" for retention purposes is a LOCAL render stamp — nothing here sends a server signal,
 * so ghost mode and missing read receipts make no difference to the archive rules.
 *
 * Per-chat overrides let you behave normally with one person while staying invisible globally.
 */
object GhostGate {

    enum class Override { USE_DEFAULT, DONT_SEND, ALWAYS_SEND }

    fun available(): Boolean = FeatureFlags.isEnabled(FeatureFlags.Feature.GHOST)

    private fun masterOn() = TrojanPrefs.getBoolean(TrojanPrefs.Keys.GHOST_ENABLED, false)

    private fun on(key: String) = available() && masterOn() &&
            TrojanPrefs.getBoolean(key, false)

    private fun override(chatId: Long, base: String): Override {
        val v = TrojanPrefs.getString("ghost.override.$chatId.$base", "USE_DEFAULT")
        return runCatching { Override.valueOf(v) }.getOrDefault(Override.USE_DEFAULT)
    }

    private fun decide(chatId: Long, prefKey: String, base: String): Boolean {
        if (!on(prefKey)) return true                       // feature off -> behave normally
        return when (override(chatId, base)) {
            Override.ALWAYS_SEND -> true
            Override.DONT_SEND -> false
            Override.USE_DEFAULT -> false                   // global rule: suppress
        }
    }

    /** true = let the read receipt go out. */
    fun shouldSendRead(chatId: Long) =
        decide(chatId, TrojanPrefs.Keys.GHOST_HIDE_READ, "read")

    fun shouldSendOnline() = !on(TrojanPrefs.Keys.GHOST_HIDE_ONLINE)

    fun shouldSendTyping(chatId: Long) =
        decide(chatId, TrojanPrefs.Keys.GHOST_HIDE_TYPING, "typing")

    fun shouldSendStoryRead() = !on(TrojanPrefs.Keys.GHOST_HIDE_STORIES)

    fun setOverride(chatId: Long, base: String, value: Override) =
        TrojanPrefs.setString("ghost.override.$chatId.$base", value.name)
}
