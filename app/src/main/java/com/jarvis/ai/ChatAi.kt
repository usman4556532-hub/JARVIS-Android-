package com.jarvis.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChatMessage(val fromUser: Boolean, val text: String)

// Talks to Gemini for the chat screen. Call reply() from a background thread.
class ChatAi {

    private val models = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )
    private var working: String? = null

    private val system =
        "You are JARVIS, a friendly and clear AI assistant inside an Android app. " +
            "Reply in the same language and script the user writes in (Urdu script, Roman Urdu or English). " +
            "Use plain text only: no markdown, no stars, no headings. " +
            "Keep answers short and helpful unless the user asks for more. " +
            "If you do not know something or are not sure, say so. " +
            "Never claim that you did something on the user's phone."

    fun reply(history: List<ChatMessage>): String {
        val key = BuildConfig.GEMINI_KEY
        if (key.isEmpty()) throw Exception("AI ki key is app mein nahi hai")
        val recent = history.takeLast(20).dropWhile { !it.fromUser }
        var problem = ""
        val first = working
        val order = if (first != null) listOf(first) + models.filter { it != first } else models
        for (m in order) {
            try {
                val t = call(m, key, recent)
                working = m
                return t
            } catch (e: Exception) {
                problem = m + ": " + (e.message ?: "masla")
            }
        }
        throw Exception(problem)
    }

    private fun call(model: String, key: String, msgs: List<ChatMessage>): String {
        val contents = JSONArray()
        for (m in msgs) {
            val parts = JSONArray().put(JSONObject().put("text", m.text))
            contents.put(JSONObject().put("role", if (m.fromUser) "user" else "model").put("parts", parts))
        }
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 2048))

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10000
        conn.readTimeout = 40000
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

        val list = JSONObject(answer)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until list.length()) {
            sb.append(list.getJSONObject(i).optString("text", ""))
        }
        val clean = sb.toString().replace("**", "").replace("\n* ", "\n- ").trim()
        if (clean.isEmpty()) throw Exception("khali jawab")
        return clean
    }
}

// FILE KHATAM
