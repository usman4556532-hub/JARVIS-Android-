package com.jarvis.ai

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.sin

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.parseColor("#0A0A18"))
        box.setPadding(pad, pad * 2, pad, pad)

        val info = TextView(this)
        info.text = "JARVIS AI\n\nMarhala 3.\n1. Mic ki ijazat dein.\n2. Default assistant chunein.\n" +
            "3. Home screen par JARVIS kholein aur boliye:\n\n" +
            "- Instagram kholo\n- Subah 7 baje alarm lagao\n- YouTube par cricket search karo\n" +
            "- Aaj ki khabrein\n- Time batao\n\nOrb ko lamba dabane se Urdu aur English badalti hai."
        info.setTextColor(SOFT_WHITE)
        info.textSize = 18f

        val permBtn = Button(this)
        permBtn.text = "Mic ki ijazat dein"
        permBtn.setOnClickListener { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1) }

        val btn = Button(this)
        btn.text = "Default assistant chunein"
        btn.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        box.addView(info)
        box.addView(permBtn)
        box.addView(btn)
        setContentView(box)
    }
}

class JarvisService : VoiceInteractionService()

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisSession(this)
}

class OrbView(context: Context) : View(context) {
    var color: Int = AQUA
        set(value) {
            field = value
            invalidate()
        }
    var pulsing: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1600
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    private fun withAlpha(c: Int, a: Int): Int = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val outer = minOf(width, height) / 2f
        val core = outer * 0.42f
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, outer, intArrayOf(withAlpha(color, 150), withAlpha(color, 0)), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, outer, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outer * 0.05f
        for (i in 0..1) {
            if (pulsing) {
                val p = (phase + i * 0.5f) % 1f
                paint.color = withAlpha(color, ((1f - p) * 190).toInt())
                canvas.drawCircle(cx, cy, core + (outer - core) * p, paint)
            } else {
                paint.color = withAlpha(color, if (i == 0) 120 else 60)
                canvas.drawCircle(cx, cy, core + (outer - core) * (0.45f + 0.45f * i), paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.color = color
        val breathe = 1f + 0.07f * sin(phase * 2f * Math.PI.toFloat())
        canvas.drawCircle(cx, cy, core * breathe, paint)
    }
}

// FILE KHATAM
