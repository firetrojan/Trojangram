/*
 * TrojanGram — preferences storage
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Single SharedPreferences file for everything TrojanGram owns.
 * Call [init] once from ApplicationLoader.onCreate().
 */
object TrojanPrefs {

    private const val FILE = "trojangram"
    private var prefs: SharedPreferences? = null

    @JvmStatic
    fun init(context: Context) {
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    private fun p(): SharedPreferences =
        prefs ?: error("TrojanPrefs.init() was never called")

    @JvmStatic
    fun getBoolean(key: String, default: Boolean) = p().getBoolean(key, default)
    fun setBoolean(key: String, value: Boolean) = p().edit().putBoolean(key, value).apply()

    @JvmStatic
    @JvmOverloads
    fun getString(key: String, default: String = "") = p().getString(key, default) ?: default
    fun setString(key: String, value: String) = p().edit().putString(key, value).apply()

    @JvmStatic
    fun getInt(key: String, default: Int) = p().getInt(key, default)
    fun setInt(key: String, value: Int) = p().edit().putInt(key, value).apply()

    /** Keys used across the app. Translation keys are user-supplied, never baked in. */
    object Keys {
        const val GLASS_ENABLED = "glass.enabled"
        const val GLASS_BLUR = "glass.blur"
        const val GLASS_TINT = "glass.tint"
        const val GLASS_ALPHA = "glass.alpha"
        const val GLASS_OUTLINE = "glass.outline"

        const val ARCHIVE_ENABLED = "archive.enabled"
        const val ARCHIVE_MARK_STYLE = "archive.mark_style"

        const val GHOST_ENABLED = "ghost.enabled"
        const val GHOST_HIDE_READ = "ghost.hide_read"
        const val GHOST_HIDE_ONLINE = "ghost.hide_online"
        const val GHOST_HIDE_TYPING = "ghost.hide_typing"
        const val GHOST_HIDE_STORIES = "ghost.hide_stories"

        const val TRANSLATE_ENABLED = "translate.enabled"
        const val TRANSLATE_PROVIDER = "translate.provider"
        const val TRANSLATE_TARGET_LANG = "translate.target_lang"
        const val KEY_DEEPL = "translate.key.deepl"
        const val KEY_AZURE = "translate.key.azure"
        const val KEY_AZURE_REGION = "translate.key.azure_region"
        const val KEY_GOOGLE_CLOUD = "translate.key.google_cloud"
        const val KEY_LLM = "translate.key.llm"
        const val KEY_LLM_ENDPOINT = "translate.key.llm_endpoint"
        const val KEY_LLM_MODEL = "translate.key.llm_model"

        const val PLUGINS_ENABLED = "plugins.enabled"
        const val PLUGINS_SAFE_MODE = "plugins.safe_mode"
        const val PLUGINS_DEV_MODE = "plugins.dev_mode"

        const val ADBLOCK_ENABLED = "browser.adblock.enabled"
        const val ADBLOCK_LIST_URL = "browser.adblock.list_url"
        const val ADBLOCK_UPDATED = "browser.adblock.updated"

        const val EXTRAS_SHOW_ID = "extras.show_id"
        const val EXTRAS_SHOW_SECONDS = "extras.show_seconds"
        const val EXTRAS_WATERMARK = "extras.watermark"
        const val EXTRAS_SILENT_BY_DEFAULT = "extras.silent_default"
        const val EXTRAS_CONFIRM_STICKER = "extras.confirm_sticker"
        const val EXTRAS_CONFIRM_VOICE = "extras.confirm_voice"
        const val EXTRAS_KEEP_ALIVE = "extras.keep_alive"
        const val EXTRAS_FONT = "extras.font"
        const val ICON_VARIANT = "icons.variant"
    }
}
