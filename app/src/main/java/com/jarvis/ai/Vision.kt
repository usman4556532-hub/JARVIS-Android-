package com.jarvis.ai

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class VisionResult(val roman: String, val urdu: String, val query: String)

// Sends a cropped picture to Gemini and gets back what it shows.
// Call look() from a background thread, never from the screen thread.
class GeminiVision {

    private val models = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )
    private var working: String? = null

    fun look(image: Bitmap): VisionResult {
        val key = BuildConfig.GEMINI_KEY
        if (key.isEmpty()) throw Exception("AI ki key is app mein nahi hai")
        val b64 = encode(image)
        var problem = ""
        val first = working
        val order = if (first != null) listOf(first) + models.filter { it != first } else models
        for (m in order) {
            try {
                val r = call(m, key, b64)
                working = m
                return r
            } catch (e: Exception) {
                problem = m + ": " + (e.message ?: "masla")
            }
        }
        throw Exception(problem)
    }

    private fun encode(src: Bitmap): String {
        val biggest = maxOf(src.width, src.height).toFloat()
        val scale = minOf(1f, 1024f / biggest)
        val bmp = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt().coerceAtLeast(1),
                (src.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            src
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun call(model: String, key: String, b64: String): VisionResult {
        val prompt =
            "This picture is a crop of an Android phone screen. Say what it shows: an app, a product, " +
                "an object, an animal, a place, or some text. " +
                "Do not name real people from their face; just describe them. " +
                "Reply ONLY with a JSON object with three fields: " +
                "roman = one or two short sentences in Roman Urdu (Urdu written with English letters), " +
                "urdu = the same sentences in Urdu script, " +
                "search = the best short Google search text for it in English, or an empty string if none."

        val parts = JSONArray()
            .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", b64)))
            .put(JSONObject().put("text", prompt))
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.3)
                    .put("maxOutputTokens", 1024)
                    .put("responseMimeType", "application/json")
            )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10000
        conn.readTimeout = 30000
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
        val j = JSONObject(raw.trim())
        return VisionResult(j.getString("roman"), j.getString("urdu"), j.optString("search", ""))
    }
}

// FILE KHATAM
