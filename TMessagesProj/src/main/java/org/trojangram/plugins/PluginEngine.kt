/*
 * TrojanGram — plugin engine
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.plugins

import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs

/**
 * Plugin engine: registry, lifecycle, safe mode, dev mode and the built-in plugins.
 *
 * Built-ins (Glass, Export, Convert Anything) are registered here as native plugins, so they
 * appear in the same list and obey the same rules as any future external plugin.
 * A scripting runtime can be added later behind [Runtime]; none is required to run.
 */
object PluginEngine {

    interface Runtime {
        fun load(plugin: Plugin): Boolean
        fun unload(plugin: Plugin)
    }

    private val plugins = LinkedHashMap<String, Plugin>()
    private val hooks = ArrayList<EventHookRecord>()
    private val menuItems = ArrayList<MenuItemRecord>()
    private val settings = ArrayList<SettingItem>()

    @Volatile var runtime: Runtime? = null

    fun enabled() = FeatureFlags.isEnabled(FeatureFlags.Feature.PLUGINS) &&
            TrojanPrefs.getBoolean(TrojanPrefs.Keys.PLUGINS_ENABLED, true)

    fun safeMode() = TrojanPrefs.getBoolean(TrojanPrefs.Keys.PLUGINS_SAFE_MODE, false)
    fun devMode() = TrojanPrefs.getBoolean(TrojanPrefs.Keys.PLUGINS_DEV_MODE, false)

    fun register(plugin: Plugin) { plugins[plugin.id] = plugin }

    fun all(): List<Plugin> = plugins.values.toList()

    fun setEnabled(id: String, on: Boolean) {
        val p = plugins[id] ?: return
        if (on && safeMode()) return                 // safe mode blocks loading
        p.enabled = on
        TrojanPrefs.setBoolean("plugin.enabled.$id", on)
        if (on) runtime?.load(p) else runtime?.unload(p)
    }

    fun addHook(record: EventHookRecord) { hooks.add(record) }
    fun addMenuItem(item: MenuItemRecord) { menuItems.add(item) }
    fun addSetting(item: SettingItem) { settings.add(item) }
    fun pluginSettings(): List<SettingItem> = settings.toList()
    fun pluginMenu(): List<MenuItemRecord> = menuItems.toList()

    /** Dispatch an app event to every enabled plugin listening for it. */
    fun dispatch(event: String, payload: Map<String, Any?> = emptyMap()) {
        if (!enabled() || safeMode()) return
        hooks.filter { it.event == event && plugins[it.pluginId]?.enabled == true }
            .forEach { runCatching { it.handler(payload) } }
    }

    /** Registered at startup. These are our own shipped features, exposed as plugins. */
    fun registerBuiltIns() {
        register(Plugin("trojangram.glass", "Liquid Glass", "1.0", "TrojanGram",
            "Cached glass rendering", trusted = true, enabled = true))
        register(Plugin("trojangram.export", "Chat Export", "1.0", "TrojanGram",
            "Export chats to HTML, JSON or TXT", trusted = true, enabled = true))
        register(Plugin("trojangram.convert", "Convert Anything", "1.0", "TrojanGram",
            "Convert media between formats", trusted = true, enabled = true))
    }
}
