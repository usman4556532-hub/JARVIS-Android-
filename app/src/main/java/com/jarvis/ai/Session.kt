package com.jarvis.ai

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class JarvisSession(ctx: Context) : VoiceInteractionSession(ctx), Screen {

    private var recognizer: SpeechRecognizer? = null
    private var urdu = true
    private var listening = false
    private var orb: OrbView? = null
    private var statusView: TextView? = null
    private var textView: TextView? = null
    private val ui = Handler(Looper.getMainLooper())
    private val brain = Brain(ctx, this)

    private fun setMode(color: Int, pulsing: Boolean, status: String, text: String) {
        orb?.color = color
        orb?.pulsing = pulsing
        statusView?.setTextColor(color)
        statusView?.text = status
        textView?.text = text
    }

    override fun thinking(heard: String, status: String) = setMode(AMBER, false, status, heard)

    override fun answer(text: String) = setMode(VIOLET, false, "Jawab", text)

    override fun launch(intent: Intent) {
        try {
            startAssistantActivity(intent)
        } catch (e: Exception) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun closeSoon() {
        ui.postDelayed({ hide() }, 600)
    }

    override fun runOnUi(block: () -> Unit) {
        ui.post { block() }
    }

    private fun langTag(): String = if (urdu) "UR" else "EN"

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listening = true
            setMode(AQUA, true, "Sun raha hoon (" + langTag() + ")", "")
        }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            listening = false
            setMode(AMBER, false, errorText(error), "Orb dabayein aur dobara bolein")
        }
        override fun onResults(results: Bundle?) {
            listening = false
            val said = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (said.isNullOrBlank()) {
                setMode(AMBER, false, "Samajh nahi aaya", "Orb dabayein aur dobara bolein")
            } else {
                brain.handle(said)
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val part = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (listening && !part.isNullOrEmpty()) textView?.text = part
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Samajh nahi aaya"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Awaaz nahi aayi"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Internet ka masla"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic ki ijazat nahi hai"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Masroof hai"
        SpeechRecognizer.ERROR_AUDIO -> "Mic ka masla"
        SpeechRecognizer.ERROR_CLIENT -> "Google awaaz service se jud nahi paya"
        else -> "Koi masla"
    }

    private fun stopRecognizer() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun startListening() {
        stopRecognizer()
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setMode(AMBER, false, "Mic ki ijazat nahi hai", "JARVIS app kholein aur ijazat dein")
            return
        }
        val google = ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
        )
        val r = SpeechRecognizer.createSpeechRecognizer(context, google)
        recognizer = r
        r.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (urdu) "ur-PK" else "en-US")
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        setMode(AQUA, true, "Tayyar ho raha hoon…", "")
        r.startListening(intent)
    }

    override fun onCreateContentView(): View {
        val dp = context.resources.displayMetrics.density
        fun px(v: Int): Int = (v * dp).toInt()

        val root = FrameLayout(context)
        root.setOnClickListener { hide() }

        val card = LinearLayout(context)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(px(12), px(10), px(14), px(10))
        card.isClickable = true
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#12122A"))
        bg.cornerRadius = px(34).toFloat()
        bg.setStroke(px(1), Color.parseColor("#B38B5CF6"))
        card.background = bg

        val orbView = OrbView(context)
        orbView.setOnClickListener { startListening() }
        orbView.setOnLongClickListener {
            urdu = !urdu
            startListening()
            true
        }
        orb = orbView

        val status = TextView(context)
        status.textSize = 13f
        status.setTextColor(AQUA)
        status.text = "Tayyar"
        statusView = status

        val text = TextView(context)
        text.textSize = 18f
        text.setTextColor(SOFT_WHITE)
        text.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        textView = text

        val column = LinearLayout(context)
        column.orientation = LinearLayout.VERTICAL
        column.addView(status)
        column.addView(text)

        val close = TextView(context)
        close.text = "✕"
        close.textSize = 16f
        close.gravity = Gravity.CENTER
        close.setTextColor(SOFT_WHITE)
        val closeBg = GradientDrawable()
        closeBg.shape = GradientDrawable.OVAL
        closeBg.setColor(Color.parseColor("#1B1B33"))
        close.background = closeBg
        close.setOnClickListener { hide() }

        card.addView(orbView, LinearLayout.LayoutParams(px(60), px(60)))
        val columnParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        columnParams.setMargins(px(10), 0, px(8), 0)
        card.addView(column, columnParams)
        card.addView(close, LinearLayout.LayoutParams(px(36), px(36)))

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        lp.setMargins(px(12), px(12), px(12), px(56))
        root.addView(card, lp)
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startListening()
    }

    override fun onHide() {
        ui.removeCallbacksAndMessages(null)
        stopRecognizer()
        super.onHide()
    }

    override fun onDestroy() {
        stopRecognizer()
        super.onDestroy()
    }
}

// FILE KHATAM
