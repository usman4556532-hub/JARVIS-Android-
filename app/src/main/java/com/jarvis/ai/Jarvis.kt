package com.jarvis.ai

import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Xml
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.sin

// ---------- colours ----------
private val AQUA = Color.parseColor("#22D3EE")
private val AMBER = Color.parseColor("#FBBF24")
private val VIOLET = Color.parseColor("#8B5CF6")
private val SOFT_WHITE = Color.parseColor("#F1F2FF")

// ---------- words JARVIS understands (Urdu and English) ----------
private val NEWS_WORDS = listOf("news", "khabar", "خبر", "نیوز", "headline", "ہیڈ لائن", "ہیڈلائن")
private val ALARM_WORDS = listOf("alarm", "الارم", "الارام", "ایلارم", "جگا")
private val SEARCH_WORDS = listOf("search", "سرچ", "تلاش", "ڈھونڈ", "dhoond", "dhund")
private val DATE_WORDS = listOf("date", "تاریخ", "tareekh", "tarikh")
private val TIME_WORDS = listOf("time", "ٹائم", "وقت", "waqt", "baje", "بجے")
private val OPEN_WORDS = listOf("open", "kholo", "khol", "کھول", "اوپن", "chalao", "چلاؤ")

private val BAJE_WORDS = setOf("baje", "بجے", "bajay", "بجکر")
private val AM_WORDS = setOf("am", "صبح", "subah")
private val PM_WORDS = setOf("pm", "شام", "sham", "shaam", "raat", "رات", "dopahar", "دوپہر")

private val NUMBER_WORDS = mapOf(
    "ایک" to 1, "تین" to 3, "چار" to 4, "پانچ" to 5, "چھ" to 6, "سات" to 7,
    "آٹھ" to 8, "نو" to 9, "دس" to 10, "گیارہ" to 11, "بارہ" to 12,
    "ek" to 1, "teen" to 3, "char" to 4, "panch" to 5, "chhe" to 6, "che" to 6,
    "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10, "gyarah" to 11, "barah" to 12
)

// Urdu names of apps -> the English name the app has on the phone
private val APP_ALIASES = mapOf(
    "instagram" to listOf("انسٹاگرام", "انسٹا گرام", "insta"),
    "youtube" to listOf("یوٹیوب", "یو ٹیوب", "you tube"),
    "whatsapp" to listOf("واٹس ایپ", "واٹس اپ", "واٹسایپ", "واٹساپ", "whats app"),
    "chrome" to listOf("کروم"),
    "spotify" to listOf("سپوٹیفائی", "اسپاٹیفائی"),
    "camera" to listOf("کیمرہ", "کیمرا"),
    "settings" to listOf("سیٹنگ", "سیٹنگز"),
    "facebook" to listOf("فیس بک", "فیسبک"),
    "photos" to listOf("فوٹوز", "تصویریں"),
    "gallery" to listOf("گیلری"),
    "calculator" to listOf("کیلکولیٹر", "کیلکیولیٹر"),
    "clock" to listOf("گھڑی", "کلاک"),
    "phone" to listOf("فون"),
    "maps" to listOf("میپس", "نقشہ"),
    "play store" to listOf("پلے اسٹور", "پلے سٹور"),
    "gmail" to listOf("جی میل", "جیمیل"),
    "telegram" to listOf("ٹیلیگرام", "ٹیلی گرام"),
    "quranify" to listOf("قرآنیفائی"),
    "flipkart" to listOf("فلپکارٹ", "فلپ کارٹ")
)

// The app's own screen: asks for the microphone permission and opens the assistant settings.
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dp = resources.displayMetrics.density
        val pad = (24 * dp).toInt()

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setBackgroundColor(Color.parseColor("#0A0A18"))
        box.setPadding(pad, pad * 2, pad, pad)

        val info = TextView(this)
        info.text = "JARVIS AI\n\nMarhala 3.\n1. Mic ki ijazat dein.\n2. Default assistant chunein.\n" +
            "3. Home screen par JARVIS kholein aur boliye:\n\n" +
            "- Instagram kholo\n- Subah 7 baje alarm lagao\n- YouTube par cricket search karo\n" +
            "- Aaj ki khabrein\n- Time batao\n\n" +
            "Orb ko lamba dabane se Urdu aur English badalti hai."
        info.setTextColor(SOFT_WHITE)
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

// The glowing orb. It pulses while listening and breathes the rest of the time.
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

        // soft glow
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, outer,
            intArrayOf(withAlpha(color, 150), withAlpha(color, 0)),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, outer, paint)
        paint.shader = null

        // rings
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

        // core
        paint.style = Paint.Style.FILL
        paint.color = color
        val breathe = 1f + 0.07f * sin(phase * 2f * Math.PI.toFloat())
        canvas.drawCircle(cx, cy, core * breathe, paint)
    }
}

// The popup at the bottom: a slim bar with the orb, the words, and a close button.
class JarvisSession(ctx: Context) : VoiceInteractionSession(ctx) {

    private var recognizer: SpeechRecognizer? = null
    private var urdu = true
    private var listening = false
    private var orb: OrbView? = null
    private var statusView: TextView? = null
    private var textView: TextView? = null
    private val ui = Handler(Looper.getMainLooper())

    // ---------- what is shown on screen ----------

    private fun setMode(color: Int, pulsing: Boolean, status: String, text: String) {
        orb?.color = color
        orb?.pulsing = pulsing
        statusView?.setTextColor(color)
        statusView?.text = status
        textView?.text = text
    }

    private fun answer(text: String) = setMode(VIOLET, false, "Jawab", text)

    private fun langTag(): String = if (urdu) "UR" else "EN"

    // ---------- listening ----------

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
                handle(said)
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

        setMode(AQUA, true, "Tayyar ho raha hoon…", "")
        r.startListening(intent)
    }

    // ---------- understanding the command ----------

    private fun normalize(s: String): String {
        val sb = StringBuilder()
        for (c in s.lowercase(Locale.ROOT)) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
        return sb.toString().replace(Regex("(\\d)(am|pm)"), "\$1 \$2").trim()
    }

    private fun hasAny(text: String, words: List<String>): Boolean = words.any { text.contains(it) }

    private fun handle(raw: String) {
        val text = normalize(raw)
        setMode(AMBER, false, "Soch raha hoon…", raw)
        when {
            hasAny(text, NEWS_WORDS) -> news()
            hasAny(text, ALARM_WORDS) -> alarm(text)
            hasAny(text, SEARCH_WORDS) -> search(text)
            hasAny(text, DATE_WORDS) ->
                answer("Aaj " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date()) + " hai")
            hasAny(text, TIME_WORDS) ->
                answer("Abhi " + SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date()) + " hain")
            else -> {
                val app = findApp(text)
                if (app != null) {
                    openApp(app)
                } else if (hasAny(text, OPEN_WORDS)) {
                    answer("Is naam ki app mujhe nahi mili.")
                } else {
                    answer("Ye baat abhi samajh nahi aayi. Main app kholna, search, alarm, time, date aur khabrein kar sakta hoon.")
                }
            }
        }
    }

    // ---------- opening apps ----------

    private fun findApp(text: String): Pair<String, String>? {
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0).map {
            Pair(it.loadLabel(pm).toString(), it.activityInfo.packageName)
        }

        var best: Pair<String, String>? = null
        for (a in apps) {
            val l = a.first.lowercase()
            if (l.length >= 3 && text.contains(l) && l.length > (best?.first?.length ?: 0)) best = a
        }
        if (best != null) return best

        for ((key, names) in APP_ALIASES) {
            if (names.any { text.contains(it) }) {
                val m = apps.firstOrNull { it.first.lowercase().contains(key) }
                if (m != null) return m
            }
        }
        return null
    }

    private fun launch(i: Intent) {
        try {
            startAssistantActivity(i)
        } catch (e: Exception) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    private fun launchAndClose(i: Intent) {
        launch(i)
        ui.postDelayed({ hide() }, 600)
    }

    private fun openApp(app: Pair<String, String>) {
        val i = context.packageManager.getLaunchIntentForPackage(app.second)
        if (i == null) {
            answer(app.first + " khol nahi saka.")
            return
        }
        answer(app.first + " khol raha hoon")
        try {
            launchAndClose(i)
        } catch (e: Exception) {
            answer(app.first + " khol nahi saka.")
        }
    }

    // ---------- search ----------

    private fun cleanQuery(text: String): String {
        val skip = setOf(
            "search", "سرچ", "karo", "کرو", "kar", "کر", "do", "دو", "par", "پر", "pe", "پے",
            "mein", "میں", "me", "on", "in", "for", "ke", "کے", "liye", "لیے", "لئے",
            "youtube", "یوٹیوب", "یو", "ٹیوب", "google", "گوگل", "maps", "میپس", "web", "ویب",
            "internet", "انٹرنیٹ", "تلاش", "ڈھونڈو", "ڈھونڈ", "dhundo", "dhoondo",
            "mujhe", "مجھے", "batao", "بتاؤ", "zara", "ذرا", "jarvis"
        )
        return text.split(" ").filter { it.isNotEmpty() && it !in skip }.joinToString(" ")
    }

    private fun isBrowser(label: String): Boolean {
        val l = label.lowercase()
        return l.contains("google") || l.contains("chrome")
    }

    private fun search(text: String) {
        val query = cleanQuery(text)
        if (query.isEmpty()) {
            answer("Kya search karun? Aise bolein: YouTube par cricket search karo.")
            return
        }
        try {
            when {
                hasAny(text, listOf("youtube", "یوٹیوب", "یو ٹیوب")) -> {
                    answer("YouTube par dhoond raha hoon: " + query)
                    launchAndClose(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query)))
                    )
                }
                hasAny(text, listOf("maps", "میپس", "نقشہ", "map")) -> {
                    answer("Maps mein dhoond raha hoon: " + query)
                    launchAndClose(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))
                }
                else -> {
                    val app = findApp(text)
                    if (app != null && !isBrowser(app.first)) {
                        answer(app.first + " ke andar search abhi nahi kar sakta. Google par dhoondne ke liye bolein: Google par " + query + " search karo.")
                    } else {
                        answer("Google par dhoond raha hoon: " + query)
                        launchAndClose(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            answer("Search nahi kar saka.")
        }
    }

    // ---------- alarm ----------

    private fun parseTime(text: String): Pair<Int, Int>? {
        val words = text.split(" ").filter { it.isNotEmpty() }
        var hour = -1
        var minute = 0

        val colon = Regex("(\\d{1,2})[:.](\\d{2})").find(text)
        if (colon != null) {
            hour = colon.groupValues[1].toInt()
            minute = colon.groupValues[2].toInt()
        } else {
            for (i in words.indices) {
                val n = words[i].toIntOrNull() ?: NUMBER_WORDS[words[i]]
                val next = if (i + 1 < words.size) words[i + 1] else ""
                if (n != null && (words[i].toIntOrNull() != null || next in BAJE_WORDS)) {
                    hour = n
                    break
                }
            }
        }
        if (hour < 0 || hour > 23 || minute > 59) return null

        val pm = words.any { it in PM_WORDS }
        val am = words.any { it in AM_WORDS }
        var h = hour
        if (h <= 12) {
            if (pm) {
                h = if (h == 12) {
                    if (words.any { it == "raat" || it == "رات" }) 0 else 12
                } else {
                    h + 12
                }
            } else if (am) {
                if (h == 12) h = 0
            } else {
                // No morning/evening said: pick the next time that is still ahead today.
                val now = Calendar.getInstance()
                val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val morning = (h % 12) * 60 + minute
                h = if (morning > current) h % 12 else if (morning + 720 > current) (h % 12) + 12 else h % 12
            }
        }
        return Pair(h, minute)
    }

    private fun alarm(text: String) {
        val t = parseTime(text)
        if (t == null) {
            answer("Alarm ka waqt samajh nahi aaya. Aise bolein: subah 7 baje alarm lagao.")
            return
        }
        val h = t.first
        val m = t.second
        val label = String.format(
            Locale.ENGLISH, "%d:%02d %s",
            if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM"
        )
        try {
            val i = In
