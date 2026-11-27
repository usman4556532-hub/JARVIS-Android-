package com.jarvis.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.view.ViewGroup
import android.widget.FrameLayout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Handles the whole "circle to search" flow: screenshot -> circle -> Gemini -> answer.
class ScreenSearch(private val context: Context, private val screen: DualScreen) {

    var shot: Bitmap? = null
    var query: String = ""
    private var crop: Bitmap? = null
    private val vision = GeminiVision()
    private val detail = GeminiDetail()
    private var overlay: CircleView? = null

    fun hasCrop(): Boolean = crop != null

    // onResult(true) means there is a search text, so the "Google par dhoondo" button can be shown.
    fun begin(root: FrameLayout, onResult: (Boolean) -> Unit) {
        val s = shot
        if (s == null) {
            screen.answerDual(
                "Screen ki tasveer nahi mili. Android ki setting 'Access screenshots' ON karein, phir swipe ya power button se JARVIS kholein.",
                "اسکرین کی تصویر نہیں ملی۔"
            )
            onResult(false)
            return
        }
        cancel(root)
        query = ""
        crop = null
        screen.thinking("Cheez ko ungli se gherein", "Screen")
        val v = CircleView(context, s) { piece ->
            root.post {
                overlay?.let { root.removeView(it) }
                overlay = null
            }
            analyse(piece, onResult)
        }
        overlay = v
        root.addView(
            v, 0,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    fun cancel(root: FrameLayout) {
        overlay?.let { root.removeView(it) }
        overlay = null
    }

    private fun analyse(piece: Bitmap, onResult: (Boolean) -> Unit) {
        crop = piece
        screen.thinking("", "Dekh raha hoon…")
        Thread {
            var result: VisionResult? = null
            var problem = ""
            try {
                result = vision.look(piece)
            } catch (e: Exception) {
                problem = e.message ?: "masla"
            }
            val res = result
            val p = problem
            screen.runOnUi {
                if (res != null) {
                    query = res.query
                    screen.answerDual(res.roman, res.urdu)
                    onResult(res.query.isNotEmpty())
                } else {
                    screen.answerDual("Tasveer samajh nahi saka. " + p, "تصویر سمجھ نہیں سکا۔")
                    onResult(false)
                }
            }
        }.start()
    }

    // Asks Gemini for more details about the circled thing and shows them as text.
    fun explain() {
        val c = crop
        if (c == null) {
            screen.answerDual(
                "Pehle Screen dabayein aur kisi cheez ko gherein.",
                "پہلے اسکرین دبائیں اور کسی چیز کو گھیریں۔"
            )
            return
        }
        screen.thinking("", "Detail la raha hoon…")
        Thread {
            var text = ""
            var problem = ""
            try {
                text = detail.explain(c)
            } catch (e: Exception) {
                problem = e.message ?: "masla"
            }
            val t = text
            val p = problem
            screen.runOnUi {
                if (t.isNotEmpty()) {
                    screen.answerDual(t, "تفصیل لکھ دی ہے۔")
                } else {
                    screen.answerDual("Detail nahi mili. " + p, "تفصیل نہیں ملی۔")
                }
            }
        }.start()
    }
}

class GeminiDetail {

    private val models = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )
    private var working: String? = null

    fun explain(image: Bitmap): String {
        val key = BuildConfig.GEMINI_KEY
        if (key.isEmpty()) throw Exception("AI ki key is app mein nahi hai")
        val b64 = encode(image)
        var problem = ""
        val first = working
        val order = if (first != null) listOf(first) + models.filter { it != first } else models
        for (m in order) {
            for (useSearch in listOf(true, false)) {
                try {
                    val t = call(m, key, b64, useSearch)
                    working = m
                    return t
                } catch (e: Exception) {
                    problem = m + ": " + (e.message ?: "masla")
                }
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

    private fun call(model: String, key: String, b64: String, useSearch: Boolean): String {
        val prompt =
            "This picture is a crop of an Android phone screen. Explain what it shows in simple Roman Urdu " +
                "(Urdu written with English letters), in 4 to 6 short lines. " +
                "If it is a product, say what it is, what it is used for, and its common features. " +
                "Do not invent prices, model numbers or facts you are not sure about; write 'pakka nahi' for those. " +
                "Do not name real people from their face. " +
                "No markdown and no stars. Start each line with a dash."

        val parts = JSONArray()
            .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", b64)))
            .put(JSONObject().put("text", prompt))
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 2048))
        if (useSearch) {
            body.put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        }

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
