/*
 * TrojanGram — build-mode switch
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.core

/**
 * The ONE switch between the personal build and anything you publish.
 *
 * PUBLIC_BUILD = false  -> personal build (current)
 * PUBLIC_BUILD = true   -> every ToS-restricted feature disappears from the menu
 *                          and FeatureFlags.isEnabled() returns false for them.
 */
object BuildMode {
    const val PUBLIC_BUILD = false
    val PRIVATE_BUILD: Boolean get() = !PUBLIC_BUILD
}
