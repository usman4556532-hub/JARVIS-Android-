package com.jarvis.ai

import android.content.Context
import android.content.Intent
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun normalize(s: String): String {
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

fun hasAny(text: String, words: List<String>): Boolean = words.any { text.contains(it) }

fun timeText(): String = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())

fun dateText(): String = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date())

fun alarmLabel(h: Int, m: Int): String =
    String.format(Locale.ENGLISH, "%d:%02d %s", if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")

fun cleanQuery(text: String): String {
    val skip = setOf(
        "search", "سرچ", "karo", "کرو", "kar", "کر", "do", "دو", "par", "پر", "pe", "پے",
        "mein", "میں", "me", "on", "in", "for", "ke", "کے", "liye", "لیے", "لئے",
        "youtube", "یوٹیوب", "یو", "ٹیوب", "google", "گوگل", "maps", "میپس", "web", "ویب",
        "internet", "انٹرنیٹ", "تلاش", "ڈھونڈو", "ڈھونڈ", "dhundo", "dhoondo",
        "mujhe", "مجھے", "batao", "بتاؤ", "zara", "ذرا", "jarvis"
    )
    return text.split(" ").filter { it.isNotEmpty() && it !in skip }.joinToString(" ")
}

fun parseTime(text: String): Pair<Int, Int>? {
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
            val now = Calendar.getInstance()
            val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val morning = (h % 12) * 60 + minute
            h = if (morning > current) h % 12 else if (morning + 720 > current) (h % 12) + 12 else h % 12
        }
    }
    return Pair(h, minute)
}

fun findApp(context: Context, text: String): Pair<String, String>? {
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

fun fetchNews(): String {
    val conn = URL("https://news.google.com/rss?hl=ur&gl=PK&ceid=PK:ur").openConnection() as HttpURLConnection
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
    val parser = Xml.newPullParser()
    parser.setInput(conn.inputStream, null)
    val titles = ArrayList<String>()
    var inItem = false
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT && titles.size < 4) {
        if (event == XmlPullParser.START_TAG) {
            if (parser.name == "item") {
                inItem = true
            } else if (inItem && parser.name == "title") {
                titles.add(parser.nextText().trim())
            }
        }
        event = parser.next()
    }
    conn.disconnect()
    return titles.mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n\n")
}

// FILE KHATAM
