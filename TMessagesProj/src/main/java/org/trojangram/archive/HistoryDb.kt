/*
 * TrojanGram — local archive database
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.archive

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Local-only archive. Nothing here is ever uploaded, synced or shared.
 *
 * Table layout is deliberately simple so it survives Telegram database upgrades untouched.
 */
class HistoryDb(context: Context) :
    SQLiteOpenHelper(context, "trojangram_history.db", null, SCHEMA) {

    companion object {
        const val SCHEMA = 1
        private const val T_DELETED = "deleted_messages"
        private const val T_EDITED = "edited_messages"

        @Volatile private var instance: HistoryDb? = null
        @JvmStatic
        fun get(context: Context): HistoryDb =
            instance ?: synchronized(this) {
                instance ?: HistoryDb(context.applicationContext).also { instance = it }
            }
    }

    data class Entry(
        val id: Long = 0,
        val chatId: Long,
        val messageId: Int,
        val kind: String,
        val text: String,
        val authorId: Long,
        val capturedAt: Long,
        val eventAt: Long,
        val firstViewedAt: Long = 0
    )

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS $T_DELETED (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            chat_id INTEGER NOT NULL,
            message_id INTEGER NOT NULL,
            kind TEXT NOT NULL,
            text TEXT NOT NULL,
            author_id INTEGER NOT NULL,
            captured_at INTEGER NOT NULL,
            event_at INTEGER NOT NULL,
            first_viewed_at INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS $T_EDITED (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            chat_id INTEGER NOT NULL,
            message_id INTEGER NOT NULL,
            kind TEXT NOT NULL,
            text TEXT NOT NULL,
            author_id INTEGER NOT NULL,
            captured_at INTEGER NOT NULL,
            event_at INTEGER NOT NULL,
            first_viewed_at INTEGER NOT NULL DEFAULT 0)""")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_del_chat ON $T_DELETED(chat_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_edit_chat ON $T_EDITED(chat_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        // No migrations yet; a future schema bump adds them here.
    }

    fun insertDeleted(e: Entry) = insert(T_DELETED, e)
    fun insertEdited(e: Entry) = insert(T_EDITED, e)

    private fun insert(table: String, e: Entry): Long {
        val v = ContentValues().apply {
            put("chat_id", e.chatId)
            put("message_id", e.messageId)
            put("kind", e.kind)
            put("text", e.text)
            put("author_id", e.authorId)
            put("captured_at", e.capturedAt)
            put("event_at", e.eventAt)
            put("first_viewed_at", e.firstViewedAt)
        }
        return writableDatabase.insert(table, null, v)
    }

    fun forChat(chatId: Long): List<Entry> = query(T_DELETED, chatId) + query(T_EDITED, chatId)

    private fun query(table: String, chatId: Long): List<Entry> {
        val out = ArrayList<Entry>()
        readableDatabase.query(table, null, "chat_id = ?", arrayOf(chatId.toString()),
            null, null, "event_at DESC").use { c ->
            while (c.moveToNext()) {
                out.add(Entry(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    chatId = c.getLong(c.getColumnIndexOrThrow("chat_id")),
                    messageId = c.getInt(c.getColumnIndexOrThrow("message_id")),
                    kind = c.getString(c.getColumnIndexOrThrow("kind")),
                    text = c.getString(c.getColumnIndexOrThrow("text")),
                    authorId = c.getLong(c.getColumnIndexOrThrow("author_id")),
                    capturedAt = c.getLong(c.getColumnIndexOrThrow("captured_at")),
                    eventAt = c.getLong(c.getColumnIndexOrThrow("event_at")),
                    firstViewedAt = c.getLong(c.getColumnIndexOrThrow("first_viewed_at"))
                ))
            }
        }
        return out
    }

    /** Called when the user opens an entry — starts the +18 h window. */
    fun markViewed(table: String, id: Long) {
        val v = ContentValues().apply { put("first_viewed_at", System.currentTimeMillis()) }
        writableDatabase.update(table, v, "id = ? AND first_viewed_at = 0", arrayOf(id.toString()))
    }

    /** Applies the retention rules. Only private 1:1 rows can ever be dropped. */
    fun prune(classifier: (Long) -> ChatClassifier.Kind) {
        val now = System.currentTimeMillis()
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (table in listOf(T_DELETED, T_EDITED)) {
                val doomed = ArrayList<Long>()
                db.query(table, arrayOf("id", "chat_id", "captured_at", "first_viewed_at"),
                    null, null, null, null, null).use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val chatId = c.getLong(1)
                        val captured = c.getLong(2)
                        val viewed = c.getLong(3)
                        if (RetentionPolicy.isExpired(classifier(chatId), captured, viewed, now)) {
                            doomed.add(id)
                        }
                    }
                }
                for (id in doomed) {
                    db.delete(table, "id = ?", arrayOf(id.toString()))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearChat(chatId: Long) {
        writableDatabase.delete(T_DELETED, "chat_id = ?", arrayOf(chatId.toString()))
        writableDatabase.delete(T_EDITED, "chat_id = ?", arrayOf(chatId.toString()))
    }

    fun wipeAll() {
        writableDatabase.delete(T_DELETED, null, null)
        writableDatabase.delete(T_EDITED, null, null)
    }
}
