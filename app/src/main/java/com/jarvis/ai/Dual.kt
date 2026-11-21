package com.jarvis.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

interface DualScreen : Screen {
    fun answerDual(show: String, say: String)
}

class DualBrain(private val context: Context, private val screen: DualScreen) {

    private fun periodUrdu(h: Int): String = when (h) {
        in 5..11 -> "صبح"
        in 12..15 -> "دوپہر"
        in 16..19 -> "شام"
        else -> "رات"
    }

    private fun periodRoman(h: Int): String = when (h) {
        in 5..11 -> "subah"
        in 12..15 -> "dopahar"
        in 16..19 -> "shaam"
        else -> "raat"
    }

    private fun clockNumber(h: Int, m: Int): String {
        val h12 = if (h % 12 == 0) 12 else h % 12
        return h12.toString() + ":" + String.format(Locale.ENGLISH, "%02d", m)
    }

    private fun timeRoman(h: Int, m: Int): String = periodRoman(h) + " ke " + clockNumber(h, m) + " baje"

    private fun timeUrdu(h: Int, m: Int): String = periodUrdu(h) + " کے " + clockNumber(h, m) + " بجے"

    fun handle(raw: String) {
        val text = normalize(raw)
        screen.thinking(raw, "Soch raha hoon…")
        when {
            hasAny(text, NEWS_WORDS) -> news()
            hasAny(text, ALARM_WORDS) -> alarm(text)
            hasAny(text, SEARCH_WORDS) -> search(text)
            hasAny(text, DATE_WORDS) -> date()
            hasAny(text, TIME_WORDS) -> now()
            else -> other(text)
        }
    }

    private fun date() {
        val d = Date()
        val roman = "Aaj " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(d) + " hai"
        val say = "آج " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ur", "PK")).format(d) + " ہے"
        screen.answerDual(roman, say)
    }

    private fun now() {
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY)
        val m = c.get(Calendar.MINUTE)
        screen.answerDual("Abhi " + timeRoman(h, m) + " hain", "ابھی " + timeUrdu(h, m) + " ہیں")
    }

    private fun other(text: String) {
        val app = findApp(context, text)
        if (app != null) {
            openApp(app)
        } else if (hasAny(text, OPEN_WORDS)) {
            screen.answerDual("Is naam ki app mujhe nahi mili.", "اس نام کی ایپ مجھے نہیں ملی۔")
        } else {
            screen.answerDual(
                "Ye baat abhi samajh nahi aayi. Main app kholna, search, alarm, time, date aur khabrein kar sakta hoon.",
                "یہ بات مجھے ابھی سمجھ نہیں آئی۔ میں ایپ کھولنا، سرچ، الارم، وقت، تاریخ اور خبریں کر سکتا ہوں۔"
            )
        }
    }

    private fun openApp(app: Pair<String, String>) {
        val i = context.packageManager.getLaunchIntentForPackage(app.second)
        if (i == null) {
            screen.answerDual(app.first + " khol nahi saka.", app.first + " نہیں کھول سکا۔")
            return
        }
        screen.answerDual(app.first + " khol raha hoon", app.first + " کھول رہا ہوں")
        try {
            screen.launch(i)
            screen.closeSoon()
        } catch (e: Exception) {
            screen.answerDual(app.first + " khol nahi saka.", app.first + " نہیں کھول سکا۔")
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
            screen.answerDual(
                "Kya search karun? Aise bolein: YouTube par cricket search karo.",
                "کیا سرچ کروں؟ ایسے بولیں: یوٹیوب پر کرکٹ سرچ کرو"
            )
            return
        }
        try {
            when {
                hasAny(text, listOf("youtube", "یوٹیوب", "یو ٹیوب")) -> {
                    screen.answerDual("YouTube par dhoond raha hoon: " + query, "یوٹیوب پر ڈھونڈ رہا ہوں: " + query)
                    view("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                }
                hasAny(text, listOf("maps", "میپس", "نقشہ", "map")) -> {
                    screen.answerDual("Maps mein dhoond raha hoon: " + query, "میپس میں ڈھونڈ رہا ہوں: " + query)
                    view("geo:0,0?q=" + Uri.encode(query))
                }
                else -> {
                    val app = findApp(context, text)
                    if (app != null && !isBrowser(app.first)) {
                        screen.answerDual(
                            app.first + " ke andar search abhi nahi kar sakta. Google par dhoondne ke liye bolein: Google par " + query + " search karo.",
                            app.first + " کے اندر سرچ ابھی نہیں کر سکتا۔ گوگل پر ڈھونڈنے کے لیے بولیں: گوگل پر " + query + " سرچ کرو"
                        )
                    } else {
                        screen.answerDual("Google par dhoond raha hoon: " + query, "گوگل پر ڈھونڈ رہا ہوں: " + query)
                        view("https://www.google.com/search?q=" + Uri.encode(query))
                    }
                }
            }
        } catch (e: Exception) {
            screen.answerDual("Search nahi kar saka.", "سرچ نہیں کر سکا۔")
        }
    }

    private fun alarm(text: String) {
        val t = parseTime(text)
        if (t == null) {
            screen.answerDual(
                "Alarm ka waqt samajh nahi aaya. Aise bolein: subah 7 baje alarm lagao.",
                "الارم کا وقت سمجھ نہیں آیا۔ ایسے بولیں: صبح سات بجے الارم لگاؤ"
            )
            return
        }
        try {
            val i = Intent(AlarmClock.ACTION_SET_ALARM)
            i.putExtra(AlarmClock.EXTRA_HOUR, t.first)
            i.putExtra(AlarmClock.EXTRA_MINUTES, t.second)
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS")
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            screen.launch(i)
            screen.answerDual(
                "Alarm bhej diya: " + alarmLabel(t.first, t.second) + ". Clock app mein dekh lein.",
                "الارم بھیج دیا ہے: " + timeUrdu(t.first, t.second) + "۔ کلاک ایپ میں دیکھ لیں۔"
            )
        } catch (e: Exception) {
            screen.answerDual("Alarm nahi laga saka.", "الارم نہیں لگا سکا۔")
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
                    screen.answerDual("Khabrein nahi la saka. Internet check karein.", "خبریں نہیں لا سکا۔ انٹرنیٹ چیک کریں۔")
                } else {
                    screen.answerDual(result, result)
                }
            }
        }.start()
    }
}

// FILE KHATAM
