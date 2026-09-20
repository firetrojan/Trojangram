/*
 * TrojanGram — convert anything
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.convert

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.trojangram.archive.ChatClassifier
import org.trojangram.archive.ContentGuard

/**
 * Convert Anything — the native version of exteraGram's plugin.
 *
 * Same hard rule as export: no conversion of anything in a secret (E2E) chat.
 *
 * Conversions that only need Android's own APIs are implemented here (image -> WebP sticker).
 * Video/audio conversions go through [FfmpegBridge], which the patch file wires to Telegram's
 * bundled FFmpeg — that keeps this module compilable without touching native code.
 */
object ConvertEngine {

    enum class Kind { IMAGE_TO_STICKER, VIDEO_TO_AUDIO, VIDEO_TO_GIF, DOC_TO_PDF }

    fun allowed(chatKind: ChatClassifier.Kind) = ContentGuard.convertAllowed(chatKind)

    interface Progress { fun onProgress(percent: Int); fun onDone(out: String?); fun onError(msg: String) }

    /** Set by the Telegram-side hook; null means video/audio conversions are unavailable. */
    var ffmpeg: FfmpegBridge? = null

    interface FfmpegBridge {
        /** Returns the output path, or null on failure. */
        fun run(args: Array<String>, onProgress: (Int) -> Unit): String?
    }

    fun convert(kind: Kind, input: String, output: String, chatKind: ChatClassifier.Kind,
                progress: Progress) {
        if (!allowed(chatKind)) { progress.onError("Converting is blocked in secret chats"); return }
        if (input.isBlank() || output.isBlank()) { progress.onError("Missing file path"); return }

        when (kind) {
            Kind.IMAGE_TO_STICKER -> imageToSticker(input, output, progress)
            Kind.VIDEO_TO_AUDIO, Kind.VIDEO_TO_GIF ->
                viaFfmpeg(kind, input, output, progress)
            Kind.DOC_TO_PDF -> progress.onError("Document to PDF needs a renderer hook")
        }
    }

    /** Real, self-contained: any image -> 512 px WebP, the stickers Telegram expects. */
    private fun imageToSticker(input: String, output: String, progress: Progress) {
        try {
            val src = BitmapFactory.decodeFile(input)
                ?: return progress.onError("Cannot read image")
            val size = 512
            val scaled = Bitmap.createScaledBitmap(src, size, size, true)
            java.io.FileOutputStream(output).use {
                val ok = scaled.compress(Bitmap.CompressFormat.WEBP, 90, it)
                if (!ok) return progress.onError("WebP encode failed")
            }
            if (scaled !== src) scaled.recycle()
            src.recycle()
            progress.onProgress(100)
            progress.onDone(output)
        } catch (e: Exception) {
            progress.onError(e.message ?: "Conversion failed")
        }
    }

    private fun viaFfmpeg(kind: Kind, input: String, output: String, progress: Progress) {
        val bridge = ffmpeg ?: run {
            progress.onError("Media conversion is not wired up yet")
            return
        }
        val args = when (kind) {
            Kind.VIDEO_TO_AUDIO -> arrayOf("-i", input, "-vn", "-acodec", "libmp3lame", output)
            Kind.VIDEO_TO_GIF -> arrayOf("-i", input, "-vf", "fps=15,scale=480:-1", output)
            else -> return
        }
        val out = bridge.run(args) { progress.onProgress(it) }
        if (out == null) progress.onError("Conversion failed") else progress.onDone(out)
    }
}
