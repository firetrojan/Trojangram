/*
 * TrojanGram — translation providers
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.translate

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Translation is based on the NagramXF / Nekogram module (GPL-3.0), rewritten for TrojanGram.
 *
 * EVERY provider here needs the user's own key, entered in TrojanGram Preferences -> Translation.
 * No key is bundled. The providers that Nagram pointed at scraped endpoints or embedded someone
 * else's key — those are deliberately not included.
 */
interface Provider {
    val id: String
    fun isConfigured(): Boolean
    fun translate(text: String, from: String?, to: String): String
}

class MissingKeyException(provider: String) :
    Exception("Add your $provider key in TrojanGram Preferences -> Translation")

private fun post(url: String, headers: Map<String, String>, body: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    headers.forEach { conn.setRequestProperty(it.key, it.value) }
    conn.doOutput = true
    conn.connectTimeout = 15_000
    conn.readTimeout = 20_000
    OutputStreamWriter(conn.outputStream).use { it.write(body) }
    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
    val text = stream.bufferedReader().readText()
    conn.disconnect()
    if (code !in 200..299) throw Exception("HTTP $code: ${text.take(160)}")
    return text
}

/** DeepL — https://deepl.com -> account -> API Free (500k chars/month, no card). */
class DeepL(private val key: String) : Provider {
    override val id = "DeepL"
    override fun isConfigured() = key.isNotBlank()
    override fun translate(text: String, from: String?, to: String): String {
        if (!isConfigured()) throw MissingKeyException("DeepL")
        val host = if (key.endsWith(":fx")) "api-free.deepl.com" else "api.deepl.com"
        val body = JSONObject().apply {
            put("text", JSONArray(listOf(text)))
            put("target_lang", to.uppercase())
        }.toString()
        val res = JSONObject(post("https://$host/v2/translate",
            mapOf("Authorization" to "DeepL-Auth-Key $key"), body))
        return res.getJSONArray("translations").getJSONObject(0).getString("text")
    }
}

/** Azure Translator — F0 free tier, 2M chars/month. */
class Azure(private val key: String, private val region: String) : Provider {
    override val id = "Azure"
    override fun isConfigured() = key.isNotBlank() && region.isNotBlank()
    override fun translate(text: String, from: String?, to: String): String {
        if (!isConfigured()) throw MissingKeyException("Azure")
        val url = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=$to"
        val body = JSONArray().put(JSONObject().put("Text", text)).toString()
        val res = JSONArray(post(url, mapOf(
            "Ocp-Apim-Subscription-Key" to key,
            "Ocp-Apim-Subscription-Region" to region), body))
        return res.getJSONObject(0).getJSONArray("translations").getJSONObject(0).getString("text")
    }
}

/** Google Cloud Translation — 500k chars/month free, needs a billing account. */
class GoogleCloud(private val key: String) : Provider {
    override val id = "GoogleCloud"
    override fun isConfigured() = key.isNotBlank()
    override fun translate(text: String, from: String?, to: String): String {
        if (!isConfigured()) throw MissingKeyException("Google Cloud")
        val res = JSONObject(post(
            "https://translation.googleapis.com/language/translate/v2?key=$key",
            emptyMap(),
            JSONObject().apply {
                put("q", text); put("target", to)
                if (!from.isNullOrBlank() && from != "auto") put("source", from)
                put("format", "text")
            }.toString()))
        return res.getJSONObject("data").getJSONArray("translations").getJSONObject(0)
            .getString("translatedText")
    }
}

/** Any OpenAI-compatible endpoint: Groq, Gemini's compatible gateway, OpenRouter, local models. */
class OpenAiCompatible(
    private val key: String,
    private val endpoint: String,
    private val model: String
) : Provider {
    override val id = "LLM"
    override fun isConfigured() = key.isNotBlank() && endpoint.isNotBlank()
    override fun translate(text: String, from: String?, to: String): String {
        if (!isConfigured()) throw MissingKeyException("LLM")
        val prompt = "Translate the following message into $to. " +
            "Reply with the translation only, keep emoji, newlines and @mentions intact.\n\n$text"
        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.2)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "user").put("content", prompt))
            })
        }.toString()
        val res = JSONObject(post(endpoint,
            mapOf("Authorization" to "Bearer $key"), body))
        return res.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            .getString("content").trim()
    }
}

/**
 * Telegram's own translation. Works only on accounts with Premium — no key involved, so we
 * surface a clear message instead of failing silently.
 */
class TelegramApi(private val caller: ((String, String, String) -> String?)? = null) : Provider {
    override val id = "Telegram"
    override fun isConfigured() = caller != null
    override fun translate(text: String, from: String?, to: String): String =
        caller?.invoke(text, from ?: "auto", to)
            ?: throw Exception("Telegram translation needs Premium and the app hook")
}
