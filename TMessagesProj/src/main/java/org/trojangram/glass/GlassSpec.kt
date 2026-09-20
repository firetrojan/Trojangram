/*
 * TrojanGram — glass parameters
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.glass

/**
 * Everything that defines how a glass surface looks. Two surfaces with the same spec
 * share one cached blur, which is what keeps the effect free.
 */
data class GlassSpec(
    val blurRadiusPx: Int = 18,
    val tintColor: Int = 0xFF101820.toInt(),
    val tintAlpha: Int = 35,          // 0..100
    val outlineStyle: Outline = Outline.SOLID,
    val cornerRadiusPx: Int = 24,
    val enabled: Boolean = true
) {
    enum class Outline { SOLID, GLARE, HIDDEN }

    /** Cache key — a change to any of these invalidates the cached blur. */
    fun key(): String =
        "$blurRadiusPx|$tintColor|$tintAlpha|$outlineStyle|$cornerRadiusPx"

    companion object {
        fun fromPrefs(
            enabled: Boolean, blur: Int, alpha: Int, outline: String
        ) = GlassSpec(
            blurRadiusPx = blur.coerceIn(0, 40),
            tintAlpha = alpha.coerceIn(0, 100),
            outlineStyle = when (outline) {
                "Glare" -> Outline.GLARE
                "Hidden" -> Outline.HIDDEN
                else -> Outline.SOLID
            },
            enabled = enabled
        )
    }
}
