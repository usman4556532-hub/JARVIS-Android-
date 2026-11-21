package com.jarvis.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Decides if a sentence is a phone command (app, alarm, search...) or just talk for the AI.
fun isCommand(context: Context, text: String): Boolean {
    if (hasAny(text, NEWS_WORDS) || hasAny(text, ALARM_WORDS) || hasAny(text, SEARCH_WORDS) ||
        hasAny(text, DATE_WORDS) || hasAny(text, TIME_WORDS)
    ) return true
    val words = text.split(" ").filter { it.isNotEmpty() }.size
    val open = hasAny(text, OPEN_WORDS)
    if (open && words <= 4) return true
    val app = findApp(context, text)
    return app != null && (open || words <= 2)
}

class GeminiChat(private val screen: DualScreen) {

    private val models = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )
    private var working: String? = null
    private val history = ArrayList<Pair<String, String>>()

    private val system =
        "You are JARVIS, a friendly voice assistant on the user's Android phone. " +
            "The user speaks Urdu or English. Answer in one to three short sentences. " +
            "Reply ONLY with a JSON object with two fields: " +
            "roman = your reply in Roman Urdu (Urdu written with English letters), " +
            "urdu = the same reply in Urdu script. " +
            "You cannot open apps, set alarms or search the web yourself in this reply. " +
            "Never claim you did something on the phone. If you do not know something, say so."

    fun ask(userText: String) {
        screen.thinking(userText, "AI soch raha hai…")
        Thread {
            var roman: String? = null
            var urdu: String? = null
            var problem = ""
            val key = BuildConfig.GEMINI_KEY
            if (key.isEmpty()) {
                problem = "AI ki key is app mein nahi hai"
            } else {
                val first = working
                val order = if (first != null) listOf(first) + models.filter { it != first } else models
                for (m in order) {
                    try {
                        val reply = call(m, key, userText)
                        roman = reply.first
                        urdu = reply.second
                        working = m
                        break
                    } catch (e: Exception) {
                        problem = m + ": " + (e.message ?: "masla")
                    }
                }
            }
            val r = roman
            val u = urdu
            val p = problem
            screen.runOnUi {
                if (r != null && u != null) {
                    history.add(Pair(userText, r))
                    if (history.size > 4) history.removeAt(0)
                    screen.answerDual(r, u)
                } else {
                    screen.answerDual("AI se jawab nahi mila. " + p, "اے آئی سے جواب نہیں ملا۔")
                }
            }
        }.start()
    }

    private fun part(text: String): JSONArray = JSONArray().put(JSONObject().put("text", text))

    private fun call(model: String, key: String, text: String): Pair<String, String> {
        val contents = JSONArray()
        for (h in history) {
            contents.put(JSONObject().put("role", "user").put("parts", part(h.first)))
            contents.put(JSONObject().put("role", "model").put("parts", part(h.second)))
        }
        contents.put(JSONObject().put("role", "user").put("parts", part(text)))

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", part(system)))
            .put("contents", contents)
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 1024)
                    .put("responseMimeType", "application/json")
            )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10000
        conn.readTimeout = 25000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", key)
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val answer = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        conn.disconnect()

        if (code !in 200..299) {
            val msg = try {
                JSONObject(answer).getJSONObject("error").getString("message")
            } catch (e: Exception) {
                ""
            }
            throw Exception(code.toString() + " " + msg.take(100))
        }

        val raw = JSONObject(answer)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .getString("text")
        return try {
            val j = JSONObject(raw.trim())
            Pair(j.getString("roman"), j.getString("urdu"))
        } catch (e: Exception) {
            Pair(raw, raw)
        }
    }
}

// FILE KHATAM
