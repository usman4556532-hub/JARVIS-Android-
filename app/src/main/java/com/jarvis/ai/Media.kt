package com.jarvis.ai

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore

private val MEDIA_PLAY_WORDS = listOf(
    "play", "chalao", "چلاؤ", "چلا دو", "bajao", "بجاؤ", "sunao", "سناؤ", "سنا دو", "lagao", "لگاؤ"
)
private val MEDIA_SURAH_WORDS = listOf("surah", "sura", "سورہ", "سورت")

private val MEDIA_NOISE = setOf(
    "spotify", "سپوٹیفائی", "اسپاٹیفائی", "par", "پر", "pe", "پے", "mein", "میں", "me", "on",
    "ka", "کا", "ki", "کی", "ke", "کے", "ko", "کو", "gana", "گانا", "song", "گیت",
    "play", "chalao", "چلاؤ", "bajao", "بجاؤ", "sunao", "سناؤ", "lagao", "لگاؤ",
    "karo", "کرو", "kar", "کر", "do", "دو", "mujhe", "مجھے", "zara", "ذرا", "jarvis",
    "youtube", "یوٹیوب"
)

private fun mediaQuery(text: String): String =
    text.split(" ").filter { it.isNotEmpty() && it !in MEDIA_NOISE }.joinToString(" ")

// Plays a song on Spotify, or finds a Surah recitation on YouTube.
// Returns true if it handled the sentence, false if the sentence is about something else.
fun handleMedia(context: Context, screen: DualScreen, text: String): Boolean {
    val spotifyPlay = hasAny(text, SPOTIFY_WORDS) && hasAny(text, MEDIA_PLAY_WORDS)
    val surah = hasAny(text, MEDIA_SURAH_WORDS)
    if (!spotifyPlay && !surah) return false

    val q = mediaQuery(text)

    if (spotifyPlay) {
        if (q.isEmpty()) {
            screen.answerDual(
                "Kaun sa gana chalaun? Aise bolein: Spotify par Atif Aslam ka gana chalao.",
                "کون سا گانا چلاؤں؟"
            )
            return true
        }
        val play = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
        play.setPackage("com.spotify.music")
        play.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
        play.putExtra(SearchManager.QUERY, q)
        val search = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/" + Uri.encode(q)))
        search.setPackage("com.spotify.music")
        screen.answerDual(
            "Spotify par chala raha hoon: " + q + ". Agar gana na chale to play dabayein.",
            "اسپاٹیفائی پر چلا رہا ہوں"
        )
        try {
            screen.launch(play)
        } catch (e: Exception) {
            try {
                screen.launch(search)
            } catch (e2: Exception) {
                screen.answerDual("Spotify nahi khul saka.", "اسپاٹیفائی نہیں کھل سکا۔")
                return true
            }
        }
        screen.closeSoon()
        return true
    }

    // Surah: search for the recitation on YouTube.
    val app = findApp(context, text)
    val other = app != null && !app.first.lowercase().contains("youtube")
    val say = if (other) {
        app!!.first + " ke andar search abhi nahi ho sakta. YouTube par tilawat dhoond raha hoon: " + q
    } else {
        "YouTube par tilawat dhoond raha hoon: " + q
    }
    screen.answerDual(say, "یوٹیوب پر تلاوت ڈھونڈ رہا ہوں")
    try {
        val url = "https://www.youtube.com/results?search_query=" + Uri.encode(q + " recitation")
        screen.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        screen.closeSoon()
    } catch (e: Exception) {
        screen.answerDual("YouTube nahi khul saka.", "یوٹیوب نہیں کھل سکا۔")
    }
    return true
}

// FILE KHATAM
