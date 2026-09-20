/*
 * TrojanGram — glass drawable
 * SPDX-License-Identifier: GPL-3.0-or-later
 * Copyright (C) 2026 TrojanGram contributors
 *
 * Based on: Telegram (GPL-2.0-or-later), exteraGram (GPL-2.0), AyuGram (GPL-2.0),
 * NagramXF/Nekogram translation module (GPL-3.0). See /NOTICE for full attribution.
 */

package org.trojangram.glass

import android.graphics.*
import android.graphics.drawable.Drawable

/**
 * Draws a glass surface using the bitmap [GlassEngine] already prepared.
 * If nothing is cached yet it paints a flat tint and asks for a prefetch — it never blocks and
 * never blurs on the UI thread, which is the whole point.
 */
class GlassDrawable(var spec: GlassSpec = GlassSpec()) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val path = Path()
    private val rect = RectF()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        // Kick off the render now; the next draw will find it cached.
        GlassEngine.prefetch(bounds.width(), bounds.height(), spec)
    }

    override fun draw(canvas: Canvas) {
        if (!spec.enabled) return
        val b = bounds
        if (b.isEmpty) return
        rect.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        val r = spec.cornerRadiusPx.toFloat()

        val cached = GlassEngine.cached(b.width(), b.height(), spec)
        if (cached != null) {
            path.reset()
            path.addRoundRect(rect, r, r, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(cached, null, b, paint)
            canvas.restore()
        } else {
            paint.color = spec.tintColor
            paint.alpha = (255 * spec.tintAlpha / 100f).toInt()
            canvas.drawRoundRect(rect, r, r, paint)
            paint.alpha = 255
            GlassEngine.prefetch(b.width(), b.height(), spec)
        }

        when (spec.outlineStyle) {
            GlassSpec.Outline.SOLID -> { outlinePaint.color = 0x33FFFFFF; canvas.drawRoundRect(rect, r, r, outlinePaint) }
            GlassSpec.Outline.GLARE -> {
                outlinePaint.shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, r, r, outlinePaint)
                outlinePaint.shader = null
            }
            GlassSpec.Outline.HIDDEN -> Unit
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
