/*
 * TrojanGram — launcher icon switcher
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import org.trojangram.core.FeatureFlags
import org.trojangram.core.TrojanPrefs

/**
 * Switchable launcher icons, the way AyuGram and exteraGram ship alternate icons.
 *
 * Each variant is an <activity-alias> in AndroidManifest.xml that is disabled by default;
 * enabling one and disabling the others makes it the launcher icon. The manifest snippet is in
 * the integration notes.
 */
object LauncherIcons {

    data class Variant(val key: String, val label: String, val alias: String)

    val variants = listOf(
        Variant("Trojan", "TrojanGram", ".ui.LauncherIconTrojan"),
        Variant("Trojan Dark", "TrojanGram Dark", ".ui.LauncherIconDark"),
        Variant("Trojan Neon", "TrojanGram Neon", ".ui.LauncherIconNeon"),
        Variant("Ghost", "Ghost", ".ui.LauncherIconGhost"),
        Variant("Plane", "Paper plane", ".ui.LauncherIconPlane"),
        Variant("Stock", "Plain", ".ui.LauncherIconStock")
    )

    fun enabled() = FeatureFlags.isEnabled(FeatureFlags.Feature.LAUNCHER_ICONS)

    fun current(): Variant {
        val key = TrojanPrefs.getString(TrojanPrefs.Keys.ICON_VARIANT, "Trojan")
        return variants.firstOrNull { it.key == key } ?: variants.first()
    }

    fun apply(context: Context, variant: Variant) {
        if (!enabled()) return
        val pm = context.packageManager
        variants.forEach { v ->
            val cn = ComponentName(context, context.packageName + v.alias)
            val state = if (v.key == variant.key)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
        }
        TrojanPrefs.setString(TrojanPrefs.Keys.ICON_VARIANT, variant.key)
    }
}
