/*
 * TrojanGram — 36h + 18h retention rules
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.archive

import org.trojangram.archive.ChatClassifier.Kind

/**
 * Retention, exactly as specified:
 *
 *  - private human-to-human 1:1 chats are the ONLY time-bounded case
 *  - 36 hours from the moment the deletion is caught from the server
 *  - when the user first opens the entry, it gets +18 hours from that moment
 *  - an entry that was never opened is never deleted
 *  - groups, channels, bot chats and chats where messaging costs Stars: no time bound
 *  - Premium does not lift or extend any timer
 */
object RetentionPolicy {

    private const val HOUR = 60L * 60L * 1000L
    const val BASE_TTL_MS = 36 * HOUR
    const val AFTER_VIEW_MS = 18 * HOUR

    /** Cut-off for a row captured at [capturedAt], first viewed at [firstViewedAt] (0 = never). */
    fun expiresAt(kind: Kind, capturedAt: Long, firstViewedAt: Long): Long {
        if (!ChatClassifier.isTimeBounded(kind)) return Long.MAX_VALUE   // kept until cleared
        return if (firstViewedAt == 0L) {
            Long.MAX_VALUE                       // unseen: we never delete it
        } else {
            maxOf(capturedAt + BASE_TTL_MS, firstViewedAt + AFTER_VIEW_MS)
        }
    }

    fun isExpired(kind: Kind, capturedAt: Long, firstViewedAt: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (!ChatClassifier.isTimeBounded(kind)) return false
        if (firstViewedAt == 0L) return false            // unseen is never expired
        return now >= expiresAt(kind, capturedAt, firstViewedAt)
    }

    /** True once the 36 h base window passed, used to nudge "unseen but aging" entries. */
    fun isAging(capturedAt: Long, now: Long = System.currentTimeMillis()) =
        now - capturedAt >= BASE_TTL_MS
}
