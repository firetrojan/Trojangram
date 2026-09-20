/*
 * TrojanGram — chat export
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.export

import org.json.JSONArray
import org.json.JSONObject
import org.trojangram.archive.ChatClassifier
import org.trojangram.archive.ContentGuard

/**
 * Export chat — the native version of exteraGram's ChatExport.
 *
 * HARD RULE: secret (E2E) chats can never be exported. The guard is checked here and again
 * wherever the menu item is built, so removing one check still leaves the other.
 */
object ExportEngine {

    enum class Format { HTML, JSON, TXT }

    data class Msg(
        val id: Int, val from: String, val date: Long, val text: String,
        val hasMedia: Boolean = false, val ttl: Int = 0, val viewOnce: Boolean = false
    )

    data class Chat(
        val id: Long, val title: String, val kind: ChatClassifier.Kind, val messages: List<Msg>
    )

    fun allowed(chat: Chat): Boolean = ContentGuard.exportAllowed(chat.kind)

    fun export(chat: Chat, format: Format): String {
        require(allowed(chat)) { "Export is blocked in secret chats" }

        val safe = chat.messages.filter {
            ContentGuard.mayCapture(chat.kind, ContentGuard.Message(
                text = it.text, hasMedia = it.hasMedia, ttlSeconds = it.ttl, isViewOnce = it.viewOnce))
        }

        return when (format) {
            Format.JSON -> toJson(chat, safe)
            Format.HTML -> toHtml(chat, safe)
            Format.TXT -> toTxt(chat, safe)
        }
    }

    private fun toJson(chat: Chat, msgs: List<Msg>): String {
        val arr = JSONArray()
        msgs.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("from", m.from); put("date", m.date)
                put("text", ContentGuard.sanitise(ContentGuard.Message(m.text)))
            })
        }
        return JSONObject().apply {
            put("chat", chat.title); put("exported_at", System.currentTimeMillis())
            put("messages", arr)
        }.toString(2)
    }

    private fun toHtml(chat: Chat, msgs: List<Msg>): String = buildString {
        append("<!doctype html><meta charset=\"utf-8\"><title>")
        append(escape(chat.title))
        append("</title><style>body{font:15px/1.5 system-ui;max-width:44em;margin:2em auto}")
        append(".m{margin:.6em 0}.f{font-weight:600}.t{color:#666;font-size:.8em}</style>")
        append("<h1>").append(escape(chat.title)).append("</h1>")
        msgs.forEach { m ->
            append("<div class=\"m\"><span class=\"f\">").append(escape(m.from))
            append("</span> <span class=\"t\">").append(m.date).append("</span><br>")
            append(escape(ContentGuard.sanitise(ContentGuard.Message(m.text)))).append("</div>")
        }
    }

    private fun toTxt(chat: Chat, msgs: List<Msg>): String = buildString {
        appendLine(chat.title); appendLine()
        msgs.forEach { m ->
            appendLine("[${m.date}] ${m.from}: ${ContentGuard.sanitise(ContentGuard.Message(m.text))}")
        }
    }

    private fun escape(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;")
}
