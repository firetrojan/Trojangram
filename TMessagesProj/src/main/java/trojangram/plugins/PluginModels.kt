/*
 * TrojanGram — plugin data model
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.plugins

/**
 * The plugin contract, modelled on exteraGram's engine (GPL-2.0) and re-implemented here.
 *
 * A plugin declares hooks, menu items and settings. The engine never lets a plugin touch
 * secret chats: every content callback goes through the same guard as the rest of the app.
 */
data class Plugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val description: String? = null,
    val trusted: Boolean = false,
    var enabled: Boolean = false
)

/** A setting row a plugin wants shown inside TrojanGram Preferences. */
data class SettingItem(
    val key: String,
    val title: String,
    val type: Type = Type.BOOL,
    val default: String = "",
    val options: List<String> = emptyList()
) {
    enum class Type { BOOL, TEXT, SELECT, SLIDER }
}

data class HookRecord(val id: String, val event: String, val handler: (Map<String, Any?>) -> Unit)
data class EventHookRecord(val event: String, val pluginId: String, val handler: (Map<String, Any?>) -> Unit)
data class MenuItemRecord(val pluginId: String, val title: String, val action: () -> Unit)
