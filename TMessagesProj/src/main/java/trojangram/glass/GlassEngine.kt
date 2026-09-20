/*
 * TrojanGram — cached blur engine
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.glass

import android.graphics.*
import android.os.Build
import android.util.LruCache
import java.util.concurrent.Executors

/**
 * Why this exists: exteraGram's glass re-blurs on every layout pass, which is what makes a chat
 * take 3–5 seconds to open. Here the blur is rendered ONCE per (size, spec) on a background
 * thread and cached, so the UI thread only ever draws an already-finished bitmap.
 *
 * Usage from a chat screen:
 *   GlassEngine.prefetch(width, height, spec)   // background, safe to call early
 *   drawable.spec = spec                        // draws cached result, or a flat tint if not ready
 */
object GlassEngine {

    private const val MAX_ENTRIES = 12
    private val cache = object : LruCache<String, Bitmap>(MAX_ENTRIES * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }
    private val worker = Executors.newSingleThreadExecutor()
    private val inFlight = HashSet<String>()

    // Downscale factor for the blur pass — blurring a quarter-size bitmap is ~16x cheaper
    // and visually identical.
    private const val DOWNSCALE = 4

    fun cached(width: Int, height: Int, spec: GlassSpec): Bitmap? =
        cache.get(cacheKey(width, height, spec))

    /**
     * Render the blur for this size/spec. Safe to call from any thread; the actual work always
     * happens off the main thread. Calling twice for the same key is a no-op.
     */
    @JvmStatic
    @JvmOverloads
    fun prefetch(width: Int, height: Int, spec: GlassSpec, source: Bitmap? = null) {
        if (!spec.enabled || width <= 0 || height <= 0) return
        val key = cacheKey(width, height, spec)
        synchronized(inFlight) {
            if (cache.get(key) != null || !inFlight.add(key)) return
        }
        worker.execute {
            try {
                val bitmap = render(width, height, spec, source)
                synchronized(cache) { cache.put(key, bitmap) }
            } catch (ignored: Throwable) {
                // Never crash the UI for a decoration.
            } finally {
                synchronized(inFlight) { inFlight.remove(key) }
            }
        }
    }

    fun invalidate() { cache.evictAll() }

    private fun cacheKey(w: Int, h: Int, spec: GlassSpec) = "${w}x$h|" + spec.key()

    private fun render(w: Int, h: Int, spec: GlassSpec, source: Bitmap?): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Base tint — even without a source we return something usable.
        canvas.drawColor(spec.tintColor)
        canvas.drawARGB(0, 0, 0, 0)

        if (source != null && spec.blurRadiusPx > 0) {
            val small = Bitmap.createScaledBitmap(
                source, (w / DOWNSCALE).coerceAtLeast(1), (h / DOWNSCALE).coerceAtLeast(1), true)
            // NOTE: a RenderEffect only takes visible effect when a RenderNode carrying it is
            // drawn, and Canvas.drawRenderNode is supported on hardware canvases only. This
            // renderer runs off the UI thread into a plain ARGB_8888 Bitmap backed by a
            // software canvas, so the box blur is used on every Android version here.
            // (Paint has no setRenderEffect at all — verified against the API 36 android.jar.)
            val blurred =
                fastBoxBlur(small, (spec.blurRadiusPx / DOWNSCALE).coerceAtLeast(1))
            canvas.drawBitmap(blurred, null, Rect(0, 0, w, h), null)
            if (blurred !== small) blurred.recycle()
            small.recycle()
        }

        val alpha = (255 * spec.tintAlpha / 100f).toInt()
        canvas.drawColor(Color.argb(alpha, Color.red(spec.tintColor),
            Color.green(spec.tintColor), Color.blue(spec.tintColor)))
        return out
    }

    /** Stack-blur style box pass — the only blur path, see note in render(). */
    private fun fastBoxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width; val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        val div = radius * 2 + 1
        for (y in 0 until h) {
            var r = 0; var g = 0; var b = 0
            for (i in -radius..radius) {
                val p = pix[y * w + i.coerceIn(0, w - 1)]
                r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (x in 0 until w) {
                out[y * w + x] = (0xFF shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                val add = pix[y * w + (x + radius).coerceIn(0, w - 1)]
                val sub = pix[y * w + (x - radius).coerceIn(0, w - 1)]
                r += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                b += (add and 0xFF) - (sub and 0xFF)
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }
}
