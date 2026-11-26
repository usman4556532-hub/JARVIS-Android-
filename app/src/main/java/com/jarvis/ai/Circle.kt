package com.jarvis.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

// Shows the screenshot and lets the user draw a circle around something.
// When the finger lifts, it gives back the cropped picture of that area.
class CircleView(
    context: Context,
    private val shot: Bitmap,
    private val onCropped: (Bitmap) -> Unit
) : View(context) {

    private val path = Path()
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dim = Paint().apply { color = Color.argb(110, 0, 0, 0) }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = AQUA
        strokeWidth = 6f * context.resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var left = 0f
    private var top = 0f
    private var right = 0f
    private var bottom = 0f

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(shot, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), pic)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)
        canvas.drawPath(path, line)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                path.reset()
                path.moveTo(e.x, e.y)
                left = e.x
                right = e.x
                top = e.y
                bottom = e.y
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(e.x, e.y)
                if (e.x < left) left = e.x
                if (e.x > right) right = e.x
                if (e.y < top) top = e.y
                if (e.y > bottom) bottom = e.y
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(e.x, e.y)
                done()
            }
        }
        invalidate()
        return true
    }

    private fun done() {
        val small = 24f * resources.displayMetrics.density
        if (right - left < small || bottom - top < small) {
            path.reset()
            return
        }
        try {
            val sx = shot.width.toFloat() / width
            val sy = shot.height.toFloat() / height
            val padX = (right - left) * 0.12f
            val padY = (bottom - top) * 0.12f
            val x1 = ((left - padX) * sx).toInt().coerceIn(0, shot.width - 1)
            val y1 = ((top - padY) * sy).toInt().coerceIn(0, shot.height - 1)
            val x2 = ((right + padX) * sx).toInt().coerceIn(x1 + 1, shot.width)
            val y2 = ((bottom + padY) * sy).toInt().coerceIn(y1 + 1, shot.height)
            onCropped(Bitmap.createBitmap(shot, x1, y1, x2 - x1, y2 - y1))
        } catch (e: Exception) {
            path.reset()
        }
    }
}

// FILE KHATAM
