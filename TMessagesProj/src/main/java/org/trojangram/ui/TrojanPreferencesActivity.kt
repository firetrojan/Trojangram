/*
 * TrojanGram — TrojanGram Preferences screen
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.trojangram.core.TrojanPrefs
import org.trojangram.core.BuildMode

/**
 * The single "TrojanGram Preferences" screen.
 *
 * It renders [SettingsRegistry] with plain Android views on purpose: no Telegram UI class is
 * touched, so it compiles and runs no matter how Telegram's internals move. If you already
 * built a nicer screen, keep it and feed it the same registry — the rows are the contract.
 */
class TrojanPreferencesActivity : Activity() {

    private val dp: Float get() = resources.displayMetrics.density
    private fun Int.dp() = (this * dp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF111418.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12.dp(), 0, 24.dp())
        }
        scroll.addView(root)

        root.addView(titleView("TrojanGram Preferences"))

        for (section in SettingsRegistry.sections) {
            val rows = section.rows.filter { SettingsRegistry.visible(it) }
            if (rows.isEmpty()) continue
            root.addView(headerView(section.title))
            rows.forEach { root.addView(rowView(it)) }
        }

        root.addView(footerView())
        setContentView(scroll)
    }

    // ---------------------------------------------------------------- chrome

    private fun titleView(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(0xFF6EE7FF.toInt())
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(20.dp(), 8.dp(), 20.dp(), 8.dp())
    }

    private fun headerView(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(0xFF8AA0B4.toInt())
        textSize = 13f
        setPadding(20.dp(), 18.dp(), 20.dp(), 6.dp())
    }

    private fun footerView() = TextView(this).apply {
        text = "GPL-3.0-or-later. Credits: Telegram, exteraGram, AyuGram, NagramXF, EasyList, uBlock Origin.\n" +
               if (BuildMode.PRIVATE_BUILD) "Personal build — restricted features visible."
               else "Public build — restricted features hidden."
        setTextColor(0xFF5A6B7B.toInt())
        textSize = 11f
        setPadding(20.dp(), 24.dp(), 20.dp(), 0)
    }

    // ---------------------------------------------------------------- rows

    private fun rowView(row: SettingsRegistry.Row): View {
        return when (row.type) {
            SettingsRegistry.Type.HEADER -> headerView(row.title)
            SettingsRegistry.Type.SWITCH -> switchRow(row)
            SettingsRegistry.Type.SLIDER -> sliderRow(row)
            SettingsRegistry.Type.SELECT -> selectRow(row)
            SettingsRegistry.Type.ACTION -> actionRow(row)
            SettingsRegistry.Type.TEXT -> textRow(row)
        }
    }

    private fun rowContainer(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 10.dp(), 20.dp(), 10.dp())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return box
    }

    private fun label(text: String, color: Int = 0xFFE6EDF3.toInt()) = TextView(this).apply {
        this.text = text; setTextColor(color); textSize = 15f
    }

    private fun subLabel(text: String) = TextView(this).apply {
        this.text = text; setTextColor(0xFF7C8C9A.toInt()); textSize = 12f
        setPadding(0, 3.dp(), 0, 0)
    }

    private fun switchRow(row: SettingsRegistry.Row): View {
        val box = rowContainer()
        val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = label(row.title).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sw = Switch(this).apply {
            isChecked = TrojanPrefs.getBoolean(row.key, row.default as? Boolean ?: false)
            setOnCheckedChangeListener { _, v -> TrojanPrefs.setBoolean(row.key, v) }
        }
        line.addView(title); line.addView(sw)
        box.addView(line)
        if (row.summary.isNotEmpty()) box.addView(subLabel(row.summary))
        return box
    }

    private fun sliderRow(row: SettingsRegistry.Row): View {
        val box = rowContainer()
        val current = TrojanPrefs.getInt(row.key, row.default as? Int ?: 0)
        val value = label("$current").apply { textSize = 13f; setTextColor(0xFF8AA0B4.toInt()) }
        box.addView(label(row.title))
        box.addView(SeekBar(this).apply {
            max = row.max
            progress = current.coerceIn(row.min, row.max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, v: Int, fromUser: Boolean) {
                    val clamped = v.coerceIn(row.min, row.max)
                    value.text = "$clamped"
                    TrojanPrefs.setInt(row.key, clamped)
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        })
        box.addView(value)
        if (row.summary.isNotEmpty()) box.addView(subLabel(row.summary))
        return box
    }

    private fun selectRow(row: SettingsRegistry.Row): View {
        val box = rowContainer()
        box.addView(label(row.title))
        val saved = TrojanPrefs.getString(row.key, row.default.toString())
        box.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@TrojanPreferencesActivity,
                android.R.layout.simple_spinner_dropdown_item, row.options)
            val index = row.options.indexOf(saved)
            if (index >= 0) setSelection(index)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) =
                    TrojanPrefs.setString(row.key, row.options[pos])
                override fun onNothingSelected(p: AdapterView<*>) {}
            }
        })
        if (row.summary.isNotEmpty()) box.addView(subLabel(row.summary))
        return box
    }

    private fun textRow(row: SettingsRegistry.Row): View {
        val box = rowContainer()
        box.addView(label(row.title))
        box.addView(EditText(this).apply {
            setText(TrojanPrefs.getString(row.key, row.default.toString()))
            setTextColor(0xFFE6EDF3.toInt())
            textSize = 14f
            if (row.secretEntry) {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                hint = "your own key — never shipped with the app"
            } else {
                inputType = InputType.TYPE_CLASS_TEXT
            }
            setHintTextColor(0xFF4A5A68.toInt())
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) TrojanPrefs.setString(row.key, text.toString())
            }
        })
        if (row.summary.isNotEmpty()) box.addView(subLabel(row.summary))
        return box
    }

    private fun actionRow(row: SettingsRegistry.Row): View {
        val box = rowContainer().apply {
            isClickable = true
            setOnClickListener { row.action?.invoke() }
        }
        box.addView(label(row.title, 0xFF6EE7FF.toInt()))
        if (row.summary.isNotEmpty()) box.addView(subLabel(row.summary))
        return box
    }
}
