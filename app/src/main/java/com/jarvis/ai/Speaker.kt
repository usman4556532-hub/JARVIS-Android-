package com.jarvis.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class MaleVoice(context: Context, private val onProblem: (String) -> Unit) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var voices: List<android.speech.tts.Voice> = emptyList()
    private var index = -1

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("ur", "PK"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    onProblem("Phone mein Urdu awaaz nahi mili")
                } else {
                    tts?.setPitch(0.75f)
                    tts?.setSpeechRate(0.95f)
                    voices = try {
                        tts?.voices?.filter { it.locale.language == "ur" }?.sortedBy { it.name } ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    ready = true
                }
            } else {
                onProblem("Awaaz ka engine shuru nahi hua")
            }
        }
    }

    fun nextVoice(): String {
        if (!ready) return "Awaaz abhi tayyar nahi"
        if (voices.size < 2) return "Is phone mein sirf ek Urdu awaaz hai"
        index = (index + 1) % voices.size
        tts?.setVoice(voices[index])
        tts?.setPitch(0.75f)
        return "Awaaz " + (index + 1) + " / " + voices.size
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

// Short talk: returns the Roman Urdu text to show and the Urdu text to speak.
fun chat(text: String): Pair<String, String>? {
    val tokens = text.split(" ")
    return when {
        hasAny(text, listOf("kya chal raha", "kya haal", "kaise ho", "kia haal", "کیا چل رہا", "کیا حال", "کیسے ہو", "کیا ہو رہا")) ->
            Pair("Main bilkul theek hoon. Aap bataiye, kya kaam hai?", "میں بالکل ٹھیک ہوں۔ آپ بتائیں، کیا کام ہے؟")
        tokens.any { it in setOf("salam", "سلام", "assalam", "السلام", "assalamualaikum") } ->
            Pair("Walaikum assalam! Main JARVIS hoon. Bataiye, kya karoon?", "وعلیکم السلام! میں جارویس ہوں۔ بتائیں، کیا کروں؟")
        hasAny(text, listOf("naam kya", "tumhara naam", "aap ka naam", "تمہارا نام", "آپ کا نام", "نام کیا")) ->
            Pair("Mera naam JARVIS hai.", "میرا نام جارویس ہے۔")
        hasAny(text, listOf("kaun ho", "kon ho", "کون ہو", "تم کون", "آپ کون")) ->
            Pair("Main JARVIS hoon, aap ka assistant.", "میں جارویس ہوں، آپ کا اسسٹنٹ۔")
        hasAny(text, listOf("shukriya", "thank", "شکریہ")) ->
            Pair("Koi baat nahi!", "کوئی بات نہیں!")
        else -> null
    }
}

// FILE KHATAM
