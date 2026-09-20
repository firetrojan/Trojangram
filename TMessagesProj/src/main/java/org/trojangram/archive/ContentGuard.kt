/*
 * TrojanGram — what may be captured
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.archive

import org.trojangram.archive.ChatClassifier.Kind

/**
 * The hard floor. These rules hold in every build, in every mode, with no switch:
 *
 *   - nothing is ever captured in secret (E2E) chats
 *   - self-destructing / view-once / TTL content is never captured
 *   - only text survives (plus a caption if it is part of the text)
 */
object ContentGuard {

    data class Message(
        val text: String?,
        val hasMedia: Boolean = false,
        val ttlSeconds: Int = 0,
        val isViewOnce: Boolean = false
    )
    @JvmStatic
    fun mayCapture(chatKind: Kind, message: Message): Boolean {
        if (chatKind == Kind.SECRET) return false
        if (message.isViewOnce || message.ttlSeconds > 0) return false
        if (message.text.isNullOrBlank()) return false
        return true
    }

    /** Media is stripped; only the text (and its caption) is stored. */
    @JvmStatic
    fun sanitise(message: Message): String =
        message.text.orEmpty().take(MAX_TEXT)

    const val MAX_TEXT = 8192

    /** Export and Convert Anything use this too — both are blocked in secret chats. */
    fun exportAllowed(chatKind: Kind) = chatKind != Kind.SECRET
    fun convertAllowed(chatKind: Kind) = chatKind != Kind.SECRET
}
