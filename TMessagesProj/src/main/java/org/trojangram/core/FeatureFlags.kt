/*
 * TrojanGram — feature gating
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.core

/**
 * Central gate for every TrojanGram feature.
 *
 * RULES THAT CANNOT BE OVERRIDDEN (true in every build):
 *  - secret (E2E) chat content is never captured, exported, converted or screenshotted
 *  - self-destructing / view-once content is never captured
 */
object FeatureFlags {

    enum class Feature {
        GLASS, EXPORT, CONVERT, ARCHIVE, GHOST, TRANSLATE, PLUGINS,
        BROWSER_ADBLOCK, EXTRAS, LAUNCHER_ICONS
    }

    /** Features that conflict with the Telegram API Terms of Service. */
    private val RESTRICTED = setOf(Feature.GHOST, Feature.ARCHIVE)

    /** Killed remotely by pref key, e.g. after a Telegram notice. */
    private const val PREF_KILLED = "trojangram.killed."

    fun isEnabled(feature: Feature): Boolean =
        !TrojanPrefs.getBoolean(PREF_KILLED + feature.name, false) &&
        (BuildMode.PRIVATE_BUILD || feature !in RESTRICTED)

    fun kill(feature: Feature) = TrojanPrefs.setBoolean(PREF_KILLED + feature.name, true)
    fun revive(feature: Feature) = TrojanPrefs.setBoolean(PREF_KILLED + feature.name, false)
}
