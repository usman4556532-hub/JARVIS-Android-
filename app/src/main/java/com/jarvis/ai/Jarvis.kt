package com.jarvis.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

// The app's own screen: explains what to do and opens the Android settings page.
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (24 * dp).toInt()

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.parseColor("#050A12"))
        box.setPadding(pad, pad * 2, pad, pad)

        val info = TextView(this)
        info.text = "JARVIS AI\n\nMarhala 1.\nNeeche wale button se Digital assistant app mein JARVIS AI chunein. " +
            "Phir neeche ke kone se swipe karein, ya power button dabaye rakhein."
        info.setTextColor(Color.parseColor("#E4F0FF"))
        info.textSize = 18f

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
        box.addView(btn)
        setContentView(box)
    }
}

// This is what makes JARVIS appear in the "Digital assistant app" list.
class JarvisService : VoiceInteractionService()

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisSession(this)
}

// The popup at the bottom. For now it only shows text (no voice yet).
class JarvisSession(ctx: Context) : VoiceInteractionSession(ctx) {
    override fun onCreateContentView(): View {
        val dp = context.resources.displayMetrics.density
        val margin = (12 * dp).toInt()
        val pad = (20 * dp).toInt()

        val root = FrameLayout(context)
        root.setOnClickListener { hide() } // tap outside the card to close

        val card = TextView(context)
        card.text = "JARVIS AI\nMarhala 1 theek chal raha hai.\nAbhi awaaz nahi hai."
        card.setTextColor(Color.parseColor("#E4F0FF"))
        card.textSize = 16f
        card.setPadding(pad, pad, pad, pad)
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#0B1420"))
        bg.cornerRadius = 28 * dp
        bg.setStroke((1 * dp).toInt(), Color.parseColor("#4FD8FF"))
        card.background = bg

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        lp.setMargins(margin, margin, margin, margin)
        root.addView(card, lp)
        return root
    }
}
