package com.jarvis.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UrduBrain(private val context: Context, private val screen: Screen) {

    private fun period(h: Int): String = when (h) {
        in 5..11 -> "صبح"
        in 12..15 -> "دوپہر"
        in 16..19 -> "شام"
        else -> "رات"
    }

    private fun clockText(h: Int, m: Int): String {
        val h12 = if (h % 12 == 0) 12 else h % 12
        return period(h) + " کے " + h12 + ":" + String.format(Locale.ENGLISH, "%02d", m) + " بجے"
    }

    private fun nowUrdu(): String {
        val c = Calendar.getInstance()
        return clockText(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    private fun dateUrdu(): String = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ur", "PK")).format(Date())

    fun handle(raw: String) {
        val text = normalize(raw)
        screen.thinking(raw, "Soch raha hoon…")
        when {
            hasAny(text, NEWS_WORDS) -> news()
            hasAny(text, ALARM_WORDS) -> alarm(text)
            hasAny(text, SEARCH_WORDS) -> search(text)
            hasAny(text, DATE_WORDS) -> screen.answer("آج " + dateUrdu() + " ہے")
            hasAny(text, TIME_WORDS) -> screen.answer("ابھی " + nowUrdu() + " ہیں")
            else -> other(text)
        }
    }

    private fun other(text: String) {
        val app = findApp(context, text)
        if (app != null) {
            openApp(app)
        } else if (hasAny(text, OPEN_WORDS)) {
            screen.answer("اس نام کی ایپ مجھے نہیں ملی۔")
        } else {
            screen.answer("یہ بات مجھے ابھی سمجھ نہیں آئی۔ میں ایپ کھولنا، سرچ، الارم، وقت، تاریخ اور خبریں کر سکتا ہوں۔")
        }
    }

    private fun openApp(app: Pair<String, String>) {
        val i = context.packageManager.getLaunchIntentForPackage(app.second)
        if (i == null) {
            screen.answer(app.first + " نہیں کھول سکا۔")
            return
        }
        screen.answer(app.first + " کھول رہا ہوں")
        try {
            screen.launch(i)
            screen.closeSoon()
        } catch (e: Exception) {
            screen.answer(app.first + " نہیں کھول سکا۔")
        }
    }

    private fun isBrowser(label: String): Boolean {
        val l = label.lowercase()
        return l.contains("google") || l.contains("chrome")
    }

    private fun view(url: String) {
        screen.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        screen.closeSoon()
    }

    private fun search(text: String) {
        val query = cleanQuery(text)
        if (query.isEmpty()) {
            screen.answer("کیا سرچ کروں؟ ایسے بولیں: یوٹیوب پر کرکٹ سرچ کرو")
            return
        }
        try {
            when {
                hasAny(text, listOf("youtube", "یوٹیوب", "یو ٹیوب")) -> {
                    screen.answer("یوٹیوب پر ڈھونڈ رہا ہوں: " + query)
                    view("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                }
                hasAny(text, listOf("maps", "میپس", "نقشہ", "map")) -> {
                    screen.answer("میپس میں ڈھونڈ رہا ہوں: " + query)
                    view("geo:0,0?q=" + Uri.encode(query))
                }
                else -> {
                    val app = findApp(context, text)
                    if (app != null && !isBrowser(app.first)) {
                        screen.answer(app.first + " کے اندر سرچ ابھی نہیں کر سکتا۔ گوگل پر ڈھونڈنے کے لیے بولیں: گوگل پر " + query + " سرچ کرو")
                    } else {
                        screen.answer("گوگل پر ڈھونڈ رہا ہوں: " + query)
                        view("https://www.google.com/search?q=" + Uri.encode(query))
                    }
                }
            }
        } catch (e: Exception) {
            screen.answer("سرچ نہیں کر سکا۔")
        }
    }

    private fun alarm(text: String) {
        val t = parseTime(text)
        if (t == null) {
            screen.answer("الارم کا وقت سمجھ نہیں آیا۔ ایسے بولیں: صبح سات بجے الارم لگاؤ")
            return
        }
        try {
            val i = Intent(AlarmClock.ACTION_SET_ALARM)
            i.putExtra(AlarmClock.EXTRA_HOUR, t.first)
            i.putExtra(AlarmClock.EXTRA_MINUTES, t.second)
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS")
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            screen.launch(i)
            screen.answer("الارم بھیج دیا ہے: " + clockText(t.first, t.second) + "۔ کلاک ایپ میں دیکھ لیں۔")
        } catch (e: Exception) {
            screen.answer("الارم نہیں لگا سکا۔")
        }
    }

    private fun news() {
        screen.thinking("", "Khabrein la raha hoon…")
        Thread {
            val result = try {
                fetchNews()
            } catch (e: Exception) {
                null
            }
            screen.runOnUi {
                if (result.isNullOrEmpty()) {
                    screen.answer("خبریں نہیں لا سکا۔ انٹرنیٹ چیک کریں۔")
                } else {
                    screen.answer(result)
                }
            }
        }.start()
    }
}

// FILE KHATAM
