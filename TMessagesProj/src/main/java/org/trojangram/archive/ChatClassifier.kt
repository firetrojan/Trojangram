/*
 * TrojanGram — chat type classification
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.archive

/**
 * Decides which retention rule a chat falls under.
 *
 * FAIL-SAFE RULE: anything we cannot classify comes back as PRIVATE, i.e. the protected,
 * time-limited case. We never guess towards "keep forever".
 */
object ChatClassifier {

    enum class Kind {
        PRIVATE_1TO1_HUMAN,   // time-limited: 36 h + 18 h
        GROUP, CHANNEL, BOT,  // kept until the user clears them
        STARS_CHARGED,        // messaging costs Stars — kept
        SECRET                // never captured at all
    }

    /** Simple description of a chat, filled by the hook in Telegram's code. */
    data class ChatInfo(
        val id: Long,
        val isSecret: Boolean = false,
        val isChannel: Boolean = false,
        val isGroup: Boolean = false,
        val isBot: Boolean = false,
        val isUser: Boolean = false,
        val starsCharged: Boolean = false
    )

        @JvmStatic
    fun classify(info: ChatInfo?): Kind {
        if (info == null) return Kind.PRIVATE_1TO1_HUMAN           // fail safe
        if (info.isSecret) return Kind.SECRET
        if (info.starsCharged) return Kind.STARS_CHARGED
        if (info.isBot) return Kind.BOT
        if (info.isChannel) return Kind.CHANNEL
        if (info.isGroup) return Kind.GROUP
        if (info.isUser) return Kind.PRIVATE_1TO1_HUMAN
        return Kind.PRIVATE_1TO1_HUMAN                              // unknown -> protected
    }

    /** Only private human-to-human 1:1 chats are time-bounded. Premium does not change this. */
    fun isTimeBounded(kind: Kind) = kind == Kind.PRIVATE_1TO1_HUMAN
}
