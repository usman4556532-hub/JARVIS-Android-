package com.jarvis.ai

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

// The app's own screen: asks for the microphone permission and opens the assistant settings.
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
        info.text = "JARVIS AI\n\nMarhala 2.\n1. Pehle mic ki ijazat dein.\n" +
            "2. Phir default assistant chunein.\n3. Home screen par JARVIS kholein aur boliye."
        info.setTextColor(Color.parseColor("#E4F0FF"))
        info.textSize = 18f

        val permBtn = Button(this)
        permBtn.text = "Mic ki ijazat dein"
        permBtn.setOnClickListener {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

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

// This is what makes JARVIS appear in the "Digital assistant app" list.
class JarvisService : VoiceInteractionService()

class JarvisSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = JarvisSession(this)
}

// The popup at the bottom. It listens through Google's speech service and shows what it heard.
class JarvisSession(ctx: Context) : VoiceInteractionSession(ctx) {

    private var recognizer: SpeechRecognizer? = null
    private var urdu = true
    private var statusView: TextView? = null
    private var heardView: TextView? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { setStatus("Sun raha hoon... boliye") }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { setStatus("Samajh raha hoon...") }
        override fun onError(error: Int) { setStatus(errorText(error) + " (" + error + ")") }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            setStatus("Maine ye suna:")
            heardView?.text = text ?: ""
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrEmpty()) heardView?.text = text
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun setStatus(text: String) {
        statusView?.text = text
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Samajh nahi aaya. Dobara bolein."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Awaaz nahi aayi. Bolo dabayein."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Internet ka masla."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic ki ijazat nahi hai."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Masroof hai. Bolo dabayein."
        SpeechRecognizer.ERROR_AUDIO -> "Mic ka masla."
        SpeechRecognizer.ERROR_CLIENT -> "Google awaaz service se jud nahi paya."
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
            setStatus("Mic ki ijazat nahi hai. JARVIS app kholein aur ijazat dein.")
            return
        }
        // We name Google's service directly, because our own placeholder service cannot listen yet.
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

        setStatus("Tayyar ho raha hoon...")
        r.startListening(intent)
    }

    override fun onCreateContentView(): View {
        val dp = context.resources.displayMetrics.density
        val margin = (12 * dp).toInt()
        val bottomGap = (56 * dp).toInt() // keeps the card above the navigation bar
        val pad = (20 * dp).toInt()

        val root = FrameLayout(context)
        root.setOnClickListener { hide() } // tap outside the card to close

        val card = LinearLayout(context)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(pad, pad, pad, pad)
        card.isClickable = true // taps inside the card do not close it
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#0B1420"))
        bg.cornerRadius = 28 * dp
        bg.setStroke((1 * dp).toInt(), Color.parseColor("#4FD8FF"))
        card.background = bg

        val title = TextView(context)
        title.text = "JARVIS AI"
        title.setTextColor(Color.parseColor("#7C93AD"))
        title.textSize = 14f

        val status = TextView(context)
        status.text = "Tayyar"
        status.setTextColor(Color.parseColor("#4FD8FF"))
        status.textSize = 18f

        val heard = TextView(context)
        heard.setTextColor(Color.parseColor("#E4F0FF"))
        heard.textSize = 22f
        heard.setPadding(0, (8 * dp).toInt(), 0, (12 * dp).toInt())

        statusView = status
        heardView = heard

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL

        val micBtn = Button(context)
        micBtn.text = "Bolo"
        micBtn.setOnClickListener { startListening() }

        val langBtn = Button(context)
        langBtn.text = "Zaban: Urdu"
        langBtn.setOnClickListener {
            urdu = !urdu
            langBtn.text = if (urdu) "Zaban: Urdu" else "Zaban: English"
            startListening()
        }

        val closeBtn = Button(context)
        closeBtn.text = "Band"
        closeBtn.setOnClickListener { hide() }

        val rowParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(micBtn, rowParams)
        row.addView(langBtn, rowParams)
        row.addView(closeBtn, rowParams)

        card.addView(title)
        card.addView(status)
        card.addView(heard)
        card.addView(row)

        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        lp.setMargins(margin, margin, margin, bottomGap)
        root.addView(card, lp)
        return root
    }

    // Each time the popup opens, it starts listening straight away.
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        startListening()
    }

    override fun onHide() {
        stopRecognizer()
        super.onHide()
    }

    override fun onDestroy() {
        stopRecognizer()
        super.onDestroy()
    }
}
