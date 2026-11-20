package com.jarvis.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Voice(context: Context, private val onProblem: (String) -> Unit) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("ur", "PK"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onProblem("Phone mein Urdu awaaz nahi mili")
                } else {
                    ready = true
                }
            } else {
                onProblem("Awaaz ka engine shuru nahi hua")
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val clean = if (text.length > 300) text.substring(0, 300) else text
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

fun smallTalk(text: String): String? {
    val tokens = text.split(" ")
    return when {
        hasAny(text, listOf("kya chal raha", "kya haal", "kaise ho", "kia haal", "کیا چل رہا", "کیا حال", "کیسے ہو", "کیا ہو رہا")) ->
            "میں بالکل ٹھیک ہوں۔ آپ بتائیں، کیا کام ہے؟"
        tokens.any { it in setOf("salam", "سلام", "assalam", "السلام", "assalamualaikum") } ->
            "وعلیکم السلام! میں جارویس ہوں۔ بتائیں، کیا کروں؟"
        hasAny(text, listOf("naam kya", "tumhara naam", "aap ka naam", "تمہارا نام", "آپ کا نام", "نام کیا")) ->
            "میرا نام جارویس ہے۔"
        hasAny(text, listOf("kaun ho", "kon ho", "کون ہو", "تم کون", "آپ کون")) ->
            "میں جارویس ہوں، آپ کا اسسٹنٹ۔"
        hasAny(text, listOf("shukriya", "thank", "شکریہ")) ->
            "کوئی بات نہیں!"
        else -> null
    }
}

// FILE KHATAM
