/*
 * TrojanGram — translation manager
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.translate

import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Front door for translation. Picks the provider from preferences, builds it from the user's own
 * key, runs off the main thread and caches results (same text + target = no second request).
 */
object TrojanTranslator {

    private val pool = Executors.newFixedThreadPool(2)
    private val cache = ConcurrentHashMap<String, String>()

    fun enabled(): Boolean = FeatureFlags.isEnabled(FeatureFlags.Feature.TRANSLATE) &&
            TrojanPrefs.getBoolean(TrojanPrefs.Keys.TRANSLATE_ENABLED, true)

    fun provider(): Provider {
        val k = TrojanPrefs.Keys
        return when (TrojanPrefs.getString(k.TRANSLATE_PROVIDER, "DeepL")) {
            "Azure" -> Azure(TrojanPrefs.getString(k.KEY_AZURE),
                TrojanPrefs.getString(k.KEY_AZURE_REGION, "westeurope"))
            "GoogleCloud" -> GoogleCloud(TrojanPrefs.getString(k.KEY_GOOGLE_CLOUD))
            "LLM" -> OpenAiCompatible(
                TrojanPrefs.getString(k.KEY_LLM),
                TrojanPrefs.getString(k.KEY_LLM_ENDPOINT),
                TrojanPrefs.getString(k.KEY_LLM_MODEL))
            "Telegram" -> TelegramApi(Hooks.telegramTranslate)
            else -> DeepL(TrojanPrefs.getString(k.KEY_DEEPL))
        }
    }

    fun targetLanguage(): String =
        TrojanPrefs.getString(TrojanPrefs.Keys.TRANSLATE_TARGET_LANG, "en").ifBlank { "en" }

    fun translate(text: String, from: String? = null, to: String = targetLanguage(),
                  onResult: (Result<String>) -> Unit) {
        if (!enabled()) { onResult(Result.failure(Exception("Translation is disabled"))); return }
        val key = "$to|$from|${text.hashCode()}"
        cache[key]?.let { onResult(Result.success(it)); return }

        pool.execute {
            val result = runCatching { provider().translate(text, from, to) }
            result.onSuccess { cache[key] = it }
            onResult(result)
        }
    }

    fun clearCache() = cache.clear()

    /** Filled by the Telegram-side hook so the "Telegram" provider can use the official API. */
    object Hooks {
        @Volatile var telegramTranslate: ((String, String, String) -> String?)? = null
    }
}
