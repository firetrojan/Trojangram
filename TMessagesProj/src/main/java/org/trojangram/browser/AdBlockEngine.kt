/*
 * TrojanGram — in-app browser ad blocking
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.browser

import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ad blocking for Telegram's built-in browser, using the open filter lists from
 * EasyList (CC BY-SA 4.0) and uBlock Origin (GPL-3.0). Credit for both is in /NOTICE.
 *
 * Scope: web pages opened inside TrojanGram only. Telegram's own sponsored messages are NOT
 * touched — that would break the API Terms of Service.
 *
 * Android's WebView has no extension API, so uBlock Origin itself cannot run here; we apply the
 * same filter lists ourselves in [shouldBlock].
 */
object AdBlockEngine {

    private val blockedHosts = HashSet<String>()
    private val exceptions = HashSet<String>()
    @Volatile var lastUpdate: Long = 0
        private set

    @JvmStatic
    fun enabled() = FeatureFlags.isEnabled(FeatureFlags.Feature.BROWSER_ADBLOCK) &&
            TrojanPrefs.getBoolean(TrojanPrefs.Keys.ADBLOCK_ENABLED, true)

    /** Call from a background thread. */
    @JvmStatic
    fun update(): Int {
        val url = TrojanPrefs.getString(TrojanPrefs.Keys.ADBLOCK_LIST_URL, DEFAULT_LIST)
        if (url.isBlank()) return 0
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            val count = parse(conn.inputStream.bufferedReader())
            conn.disconnect()
            lastUpdate = System.currentTimeMillis()
            TrojanPrefs.setInt(TrojanPrefs.Keys.ADBLOCK_UPDATED, (lastUpdate / 1000).toInt())
            count
        } catch (e: Exception) {
            -1
        }
    }

    /** Understands the parts of EasyList/uBlock syntax that matter: ||host^ and @@ exceptions. */
    fun parse(reader: BufferedReader): Int {
        blockedHosts.clear(); exceptions.clear()
        var n = 0
        reader.useLines { lines ->
            lines.forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("!")) return@forEach
                if (line.startsWith("@@")) {
                    hostOf(line.removePrefix("@@"))?.let { exceptions.add(it) }
                    return@forEach
                }
                hostOf(line)?.let { blockedHosts.add(it); n++ }
            }
        }
        return n
    }

    private fun hostOf(rule: String): String? {
        if (!rule.startsWith("||")) return null
        var host = rule.removePrefix("||")
        val cut = host.indexOfFirst { it == '^' || it == '/' || it == '$' }
        if (cut >= 0) host = host.substring(0, cut)
        if (host.isBlank() || host.contains("*")) return null
        return host.lowercase()
    }

    /** true = drop the request. Called from WebViewClient.shouldInterceptRequest. */
    @JvmStatic
    fun shouldBlock(url: String?): Boolean {
        if (!enabled() || url.isNullOrBlank()) return false
        val host = runCatching { URL(url).host.lowercase() }.getOrNull() ?: return false
        if (exceptions.any { host == it || host.endsWith(".$it") }) return false
        return blockedHosts.any { host == it || host.endsWith(".$it") }
    }

    fun ruleCount() = blockedHosts.size

    const val DEFAULT_LIST =
        "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"
}
