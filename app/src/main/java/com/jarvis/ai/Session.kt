package com.jarvis.ai

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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

class JarvisSession(ctx: Context) : VoiceInteractionSession(ctx), DualScreen {

    private var recognizer: SpeechRecognizer? = null
    private var urdu = true
    private var listening = false
    private var orb: OrbView? = null
    private var statusView: TextView? = null
    private var textView: TextView? = null
    private var rootView: FrameLayout? = null
    private var googleBtn: TextView? = null
    private var voiceProblem: String? = null
    private val ui = Handler(Looper.getMainLooper())
    private val brain = DualBrain(ctx, this)
    private val ai = GeminiChat(this)
    private val picker = AppPicker(ctx, this)
    private val shotSearch = ScreenSearch(ctx, this)
    private val voice = MaleVoice(ctx) { msg -> ui.post { voiceProblem = msg } }

    private fun setMode(color: Int, pulsing: Boolean, status: String, text: String) {
        orb?.color = color
        orb?.pulsing = pulsing
        statusView?.setTextColor(color)
        statusView?.text = status
        textView?.text = text
    }

    override fun thinking(heard: String, status: String) = setMode(AMBER, false, status, heard)

    override fun answer(text: String) {
        answerDual(text, text)
    }

    // show = Roman Urdu on screen, say = Urdu script for the voice
    override fun answerDual(show: String, say: String) {
        val problem = voiceProblem
        val status = if (problem == null) "Jawab" else "Jawab — " + problem
        setMode(VIOLET, false, status, show)
        voice.speak(say)
    }

    override fun launch(intent: Intent) {
        try {
            startAssistantActivity(intent)
        } catch (e: Exception) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun closeSoon() {
        ui.postDelayed({ hide() }, 1500)
    }

    override fun runOnUi(block: () -> Unit) {
        ui.post { block() }
    }

    // Android gives us the picture of the screen here (only if "Access screenshots" is ON).
    override fun onHandleScreenshot(screenshot: Bitmap?) {
        shotSearch.shot = screenshot
    }

    private fun langTag(): String = if (urdu) "UR" else "EN"

    private fun wantsApp(norm: String): Boolean {
        if (!hasAny(norm, OPEN_WORDS)) return false
        if (hasAny(norm, NEWS_WORDS) || hasAny(norm, ALARM_WORDS) || hasAny(norm, SEARCH_WORDS) ||
            hasAny(norm, DATE_WORDS) || hasAny(norm, TIME_WORDS)
        ) return false
        return findApp(context, norm) == null
    }

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
                val norm = normalize(said)
                val talk = chat(norm)
                if (talk != null) {
                    answerDual(talk.first, talk.second)
                } else if (wantsApp(norm)) {
                    picker.open(said)
                } else if (isCommand(context, norm)) {
                    brain.handle(said)
                } else {
                    ai.ask(said)
                }
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

    private fun closeCircle() {
        rootView?.let { shotSearch.cancel(it) }
        googleBtn?.visibility = View.GONE
    }

    private fun startListening() {
        voice.stop()
        stopRecognizer()
        closeCircle()
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
        rootView = root

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
        status.setPadding(0, px(4), px(16), px(4))
        status.setOnClickListener {
            val msg = voice.nextVoice()
            setMode(VIOLET, false, msg, "Ye meri awaaz hai")
            voice.speak("یہ میری آواز ہے")
        }
        statusView = status

        val text = TextView(context)
        text.textSize = 18f
        text.setTextColor(SOFT_WHITE)
        text.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        textView = text

        fun chip(label: String): TextView {
            val t = TextView(context)
            t.text = label
            t.textSize = 13f
            t.setTextColor(SOFT_WHITE)
            t.setPadding(px(12), px(6), px(12), px(6))
            val b = GradientDrawable()
            b.cornerRadius = px(16).toFloat()
            b.setColor(Color.parseColor("#1B1B33"))
            b.setStroke(px(1), VIOLET)
            t.background = b
            return t
        }

        val screenChip = chip("Screen")
        screenChip.setOnClickListener {
            voice.stop()
            stopRecognizer()
            googleBtn?.visibility = View.GONE
            val r = rootView
            if (r != null) {
                shotSearch.begin(r) { has -> googleBtn?.visibility = if (has) View.VISIBLE else View.GONE }
            }
        }

        val googleChip = chip("Google par dhoondo")
        googleChip.visibility = View.GONE
        googleChip.setOnClickListener {
            val q = shotSearch.query
            if (q.isNotEmpty()) {
                launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q))))
                closeSoon()
            }
        }
        googleBtn = googleChip

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.setPadd
