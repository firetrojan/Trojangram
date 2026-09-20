/*
 * TrojanGram — declarative settings rows
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.ui

import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs.Keys as K

/**
 * Every TrojanGram option lives here. exteraGram's preferences, AyuGram's preferences and
 * the translation settings are merged into ONE list — there is no second settings screen.
 *
 * Add an option = add a row. Nothing else changes.
 */
object SettingsRegistry {

    enum class Type { SWITCH, TEXT, SELECT, SLIDER, ACTION, HEADER }

    data class Row(
        val key: String,
        val title: String,
        val summary: String = "",
        val type: Type = Type.SWITCH,
        val default: Any = false,
        val options: List<String> = emptyList(),
        val min: Int = 0,
        val max: Int = 100,
        val feature: FeatureFlags.Feature? = null,
        val secretEntry: Boolean = false,
        val action: (() -> Unit)? = null
    )

    data class Section(val id: String, val title: String, val rows: List<Row>)

    /** Row is hidden when its feature is disabled or killed. */
    fun visible(row: Row): Boolean =
        row.feature == null || FeatureFlags.isEnabled(row.feature)

    val sections: List<Section> = listOf(

        Section("glass", "Glass", listOf(
            Row(K.GLASS_ENABLED, "Liquid glass", "Blur is pre-rendered and cached, so opening a chat stays instant",
                Type.SWITCH, true, feature = FeatureFlags.Feature.GLASS),
            Row(K.GLASS_BLUR, "Blur strength", "0–40 px", Type.SLIDER, 18, min = 0, max = 40,
                feature = FeatureFlags.Feature.GLASS),
            Row(K.GLASS_ALPHA, "Tint opacity", "0–100%", Type.SLIDER, 35, min = 0, max = 100,
                feature = FeatureFlags.Feature.GLASS),
            Row(K.GLASS_OUTLINE, "Outline style", "Solid, glare or hidden", Type.SELECT, "Solid",
                options = listOf("Solid", "Glare", "Hidden"), feature = FeatureFlags.Feature.GLASS)
        )),

        Section("privacy", "Privacy & Spy", listOf(
            Row(K.GHOST_ENABLED, "Ghost mode", "Master switch", Type.SWITCH, false,
                feature = FeatureFlags.Feature.GHOST),
            Row(K.GHOST_HIDE_READ, "Don't send read receipts", "", Type.SWITCH, true,
                feature = FeatureFlags.Feature.GHOST),
            Row(K.GHOST_HIDE_ONLINE, "Don't send online status", "", Type.SWITCH, true,
                feature = FeatureFlags.Feature.GHOST),
            Row(K.GHOST_HIDE_TYPING, "Don't send typing status", "", Type.SWITCH, false,
                feature = FeatureFlags.Feature.GHOST),
            Row(K.GHOST_HIDE_STORIES, "Don't mark stories as read", "", Type.SWITCH, true,
                feature = FeatureFlags.Feature.GHOST)
        )),

        Section("archive", "Deleted & Edited", listOf(
            Row(K.ARCHIVE_ENABLED, "Keep deleted and edited messages",
                "Private 1:1 chats: 36 h from capture, +18 h from first view. Groups, channels and bots are kept until you clear them",
                Type.SWITCH, true, feature = FeatureFlags.Feature.ARCHIVE),
            Row(K.ARCHIVE_MARK_STYLE, "Deleted mark", "Text, cross, eye-crossed, bin or nothing",
                Type.SELECT, "Text", options = listOf("Text", "Cross", "EyeCrossed", "TrashBin", "Nothing"),
                feature = FeatureFlags.Feature.ARCHIVE),
            Row("archive.view", "Open deleted messages", "", Type.ACTION, false,
                feature = FeatureFlags.Feature.ARCHIVE),
            Row("archive.clear", "Clear archive now", "Deletes everything immediately", Type.ACTION, false,
                feature = FeatureFlags.Feature.ARCHIVE)
        )),

        Section("translate", "Translation", listOf(
            Row(K.TRANSLATE_ENABLED, "Enable translation", "", Type.SWITCH, true,
                feature = FeatureFlags.Feature.TRANSLATE),
            Row(K.TRANSLATE_PROVIDER, "Provider", "DeepL, Azure, Google Cloud, LLM or Telegram",
                Type.SELECT, "DeepL",
                options = listOf("DeepL", "Azure", "GoogleCloud", "LLM", "Telegram"),
                feature = FeatureFlags.Feature.TRANSLATE),
            Row(K.TRANSLATE_TARGET_LANG, "Target language", "e.g. en, hi, ru", Type.TEXT, "en",
                feature = FeatureFlags.Feature.TRANSLATE),
            Row(K.KEY_DEEPL, "DeepL API key", "deepl.com → your account → API Free", Type.TEXT, "",
                feature = FeatureFlags.Feature.TRANSLATE, secretEntry = true),
            Row(K.KEY_AZURE, "Azure Translator key", "portal.azure.com → Translator → Keys",
                Type.TEXT, "", feature = FeatureFlags.Feature.TRANSLATE, secretEntry = true),
            Row(K.KEY_AZURE_REGION, "Azure region", "e.g. westeurope", Type.TEXT, "westeurope",
                feature = FeatureFlags.Feature.TRANSLATE),
            Row(K.KEY_GOOGLE_CLOUD, "Google Cloud key", "console.cloud.google.com → credentials",
                Type.TEXT, "", feature = FeatureFlags.Feature.TRANSLATE, secretEntry = true),
            Row(K.KEY_LLM, "LLM API key", "Groq, Gemini, OpenRouter…", Type.TEXT, "",
                feature = FeatureFlags.Feature.TRANSLATE, secretEntry = true),
            Row(K.KEY_LLM_ENDPOINT, "LLM endpoint", "OpenAI-compatible URL", Type.TEXT,
                "https://api.groq.com/openai/v1/chat/completions",
                feature = FeatureFlags.Feature.TRANSLATE),
            Row(K.KEY_LLM_MODEL, "LLM model", "e.g. llama-3.3-70b-versatile", Type.TEXT,
                "llama-3.3-70b-versatile", feature = FeatureFlags.Feature.TRANSLATE)
        )),

        Section("plugins", "Plugins", listOf(
            Row(K.PLUGINS_ENABLED, "Plugin engine", "", Type.SWITCH, true,
                feature = FeatureFlags.Feature.PLUGINS),
            Row(K.PLUGINS_SAFE_MODE, "Safe mode", "Load no plugin at startup", Type.SWITCH, false,
                feature = FeatureFlags.Feature.PLUGINS),
            Row(K.PLUGINS_DEV_MODE, "Developer mode", "", Type.SWITCH, false,
                feature = FeatureFlags.Feature.PLUGINS)
        )),

        Section("browser", "In-app browser", listOf(
            Row(K.ADBLOCK_ENABLED, "Block web ads",
                "Uses EasyList / uBlock filter lists. Telegram's own sponsored messages are untouched.",
                Type.SWITCH, true, feature = FeatureFlags.Feature.BROWSER_ADBLOCK),
            Row(K.ADBLOCK_LIST_URL, "Filter list URL", "", Type.TEXT,
                "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                feature = FeatureFlags.Feature.BROWSER_ADBLOCK),
            Row("browser.update", "Update filter list now", "", Type.ACTION, false,
                feature = FeatureFlags.Feature.BROWSER_ADBLOCK)
        )),

        Section("extras", "Extras", listOf(
            Row(K.EXTRAS_SHOW_ID, "Show message ID", "", Type.SWITCH, false,
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_SHOW_SECONDS, "Show seconds in timestamps", "", Type.SWITCH, false,
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_WATERMARK, "Watermark on exported media", "", Type.TEXT, "",
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_SILENT_BY_DEFAULT, "Send without sound by default", "", Type.SWITCH, false,
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_CONFIRM_STICKER, "Confirm before sending stickers", "", Type.SWITCH, false,
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_CONFIRM_VOICE, "Confirm before sending voice", "", Type.SWITCH, true,
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_KEEP_ALIVE, "Keep connection alive", "Costs battery", Type.SWITCH, false,
                feature = FeatureFlags.Feature.EXTRAS),
            Row(K.EXTRAS_FONT, "Interface font", "", Type.SELECT, "Default",
                options = listOf("Default", "System", "Monospace"), feature = FeatureFlags.Feature.EXTRAS)
        )),

        Section("icons", "App icon", listOf(
            Row(K.ICON_VARIANT, "Launcher icon", "", Type.SELECT, "Trojan",
                options = listOf("Trojan", "Trojan Dark", "Trojan Neon", "Ghost", "Plane", "Stock"),
                feature = FeatureFlags.Feature.LAUNCHER_ICONS)
        )),

        Section("about", "About", listOf(
            Row("about.version", "TrojanGram version", "", Type.TEXT, ""),
            Row("about.licences", "Open source licences", "Telegram, exteraGram, AyuGram, NagramXF, EasyList, uBlock",
                Type.ACTION, false),
            Row("about.source", "Source code", "", Type.ACTION, false)
        ))
    )
}
