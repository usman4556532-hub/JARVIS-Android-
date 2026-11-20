package com.jarvis.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

interface Screen {
    fun thinking(heard: String, status: String)
    fun answer(text: String)
    fun launch(intent: Intent)
    fun closeSoon()
    fun runOnUi(block: () -> Unit)
}

class Brain(private val context: Context, private val screen: Screen) {

    fun handle(raw: String) {
        val text = normalize(raw)
        screen.thinking(raw, "Soch raha hoon…")
        when {
            hasAny(text, NEWS_WORDS) -> news()
            hasAny(text, ALARM_WORDS) -> alarm(text)
            hasAny(text, SEARCH_WORDS) -> search(text)
            hasAny(text, DATE_WORDS) -> screen.answer("Aaj " + dateText() + " hai")
            hasAny(text, TIME_WORDS) -> screen.answer("Abhi " + timeText() + " hain")
            else -> other(text)
        }
    }

    private fun other(text: String) {
        val app = findApp(context, text)
        if (app != null) {
            openApp(app)
        } else if (hasAny(text, OPEN_WORDS)) {
            screen.answer("Is naam ki app mujhe nahi mili.")
        } else {
            screen.answer("Ye baat abhi samajh nahi aayi. Main app kholna, search, alarm, time, date aur khabrein kar sakta hoon.")
        }
    }

    private fun openApp(app: Pair<String, String>) {
        val i = context.packageManager.getLaunchIntentForPackage(app.second)
        if (i == null) {
            screen.answer(app.first + " khol nahi saka.")
            return
        }
        screen.answer(app.first + " khol raha hoon")
        try {
            screen.launch(i)
            screen.closeSoon()
        } catch (e: Exception) {
            screen.answer(app.first + " khol nahi saka.")
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
            screen.answer("Kya search karun? Aise bolein: YouTube par cricket search karo.")
            return
        }
        try {
            when {
                hasAny(text, listOf("youtube", "یوٹیوب", "یو ٹیوب")) -> {
                    screen.answer("YouTube par dhoond raha hoon: " + query)
                    view("https://www.youtube.com/results?search_query=" + Uri.encode(query))
                }
                hasAny(text, listOf("maps", "میپس", "نقشہ", "map")) -> {
                    screen.answer("Maps mein dhoond raha hoon: " + query)
                    view("geo:0,0?q=" + Uri.encode(query))
                }
                else -> {
                    val app = findApp(context, text)
                    if (app != null && !isBrowser(app.first)) {
                        screen.answer(app.first + " ke andar search abhi nahi kar sakta. Google par dhoondne ke liye bolein: Google par " + query + " search karo.")
                    } else {
                        screen.answer("Google par dhoond raha hoon: " + query)
                        view("https://www.google.com/search?q=" + Uri.encode(query))
                    }
                }
            }
        } catch (e: Exception) {
            screen.answer("Search nahi kar saka.")
        }
    }

    private fun alarm(text: String) {
        val t = parseTime(text)
        if (t == null) {
            screen.answer("Alarm ka waqt samajh nahi aaya. Aise bolein: subah 7 baje alarm lagao.")
            return
        }
        try {
            val i = Intent(AlarmClock.ACTION_SET_ALARM)
            i.putExtra(AlarmClock.EXTRA_HOUR, t.first)
            i.putExtra(AlarmClock.EXTRA_MINUTES, t.second)
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS")
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            screen.launch(i)
            screen.answer("Alarm bhej diya: " + alarmLabel(t.first, t.second) + ". Clock app mein dekh lein.")
        } catch (e: Exception) {
            screen.answer("Alarm nahi laga saka.")
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
                    screen.answer("Khabrein nahi la saka. Internet check karein.")
                } else {
                    screen.answer(result)
                }
            }
        }.start()
    }
}

// FILE KHATAM
