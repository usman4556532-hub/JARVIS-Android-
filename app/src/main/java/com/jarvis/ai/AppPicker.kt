package com.jarvis.ai

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Used when the user says "open ..." but the app name was not recognised on the phone.
// It asks Gemini which installed app the user means (this handles Urdu spellings).
class AppPicker(private val context: Context, private val screen: DualScreen) {

    private val models = listOf(
        "gemini-flash-latest",
        "gemini-flash-lite-latest",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite"
    )

    private fun installed(): List<Pair<String, String>> {
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            .map { Pair(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.second }
    }

    fun open(said: String) {
        screen.thinking(said, "App dhoond raha hoon…")
        Thread {
            val apps = installed()
            var chosen: Pair<String, String>? = null
            var problem = ""
            val key = BuildConfig.GEMINI_KEY
            if (key.isEmpty()) {
                problem = "AI ki key is app mein nahi hai"
            } else {
                for (m in models) {
                    try {
                        val name = ask(m, key, said, apps.map { it.first })
                        chosen = apps.firstOrNull { it.first.equals(name, ignoreCase = true) }
                        problem = ""
                        break
                    } catch (e: Exception) {
                        problem = m + ": " + (e.message ?: "masla")
                    }
                }
            }
            val c = chosen
            val p = problem
            screen.runOnUi {
                if (c != null) {
                    val i = context.packageManager.getLaunchIntentForPackage(c.second)
                    if (i != null) {
                        screen.answerDual(c.first + " khol raha hoon", c.first + " کھول رہا ہوں")
                        try {
                            screen.launch(i)
                            screen.closeSoon()
                        } catch (e: Exception) {
                            screen.answerDual(c.first + " khol nahi saka.", c.first + " نہیں کھول سکا۔")
                        }
                    } else {
                        screen.answerDual(c.first + " khol nahi saka.", c.first + " نہیں کھول سکا۔")
                    }
                } else if (p.isNotEmpty()) {
                    screen.answerDual("App nahi dhoond saka. " + p, "ایپ نہیں ڈھونڈ سکا۔")
                } else {
                    screen.answerDual("Is naam ki app mujhe nahi mili.", "اس نام کی ایپ مجھے نہیں ملی۔")
                }
            }
        }.start()
    }

    private fun ask(model: String, key: String, said: String, labels: List<String>): String {
        val prompt = "Installed apps on the phone, one per line:\n" + labels.joinToString("\n") +
            "\n\nThe user said (Urdu or English): " + said +
            "\n\nWhich ONE installed app does the user want to open? " +
            "Reply with a JSON object: {\"app\": \"<exact name from the list, or none>\"}"
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0)
                    .put("maxOutputTokens", 1024)
                    .put("responseMimeType", "application/json")
            )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10000
        conn.readTimeout = 20000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("x-goog-api-key", key)
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val answer = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        conn.disconnect()
        if (code !in 200..299) throw Exception(code.toString())

        val raw = JSONObject(answer)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .getString("text")
        return JSONObject(raw.trim()).getString("app")
    }
}

// FILE KHATAM
