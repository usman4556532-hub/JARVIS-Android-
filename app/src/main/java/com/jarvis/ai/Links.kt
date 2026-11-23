package com.jarvis.ai

import android.content.Intent
import android.net.Uri

val SPOTIFY_WORDS = listOf("spotify", "سپوٹیفائی", "اسپاٹیفائی")
val STORE_WORDS = listOf("play store", "playstore", "پلے اسٹور", "پلے سٹور")
val INSTA_WORDS = listOf("instagram", "انسٹاگرام", "انسٹا گرام", "insta")
val PROFILE_WORDS = listOf("profile", "پروفائل", "account", "اکاؤنٹ")

private val APP_NAME_WORDS = setOf(
    "spotify", "سپوٹیفائی", "اسپاٹیفائی", "play", "store", "playstore", "پلے", "اسٹور", "سٹور",
    "instagram", "انسٹاگرام", "انسٹا", "گرام", "insta", "profile", "پروفائل", "account", "اکاؤنٹ",
    "kholo", "khol", "کھولو", "کھول", "open", "اوپن", "dikhao", "دکھاؤ", "username", "یوزر"
)

class LinkPlan(val roman: String, val urdu: String, val intent: Intent?)

private fun clean(query: String): String =
    query.split(" ").filter { it.isNotEmpty() && it !in APP_NAME_WORDS }.joinToString(" ")

// For Spotify, Play Store and Instagram. Returns null if the sentence is about none of them.
fun linkPlan(text: String, query: String): LinkPlan? {
    val q = clean(query)
    if (hasAny(text, SPOTIFY_WORDS)) {
        if (q.isEmpty()) {
            return LinkPlan("Spotify mein kya dhoondun?", "سپوٹیفائی میں کیا ڈھونڈوں؟", null)
        }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/" + Uri.encode(q)))
        i.setPackage("com.spotify.music")
        return LinkPlan("Spotify mein dhoond raha hoon: " + q, "سپوٹیفائی میں ڈھونڈ رہا ہوں: " + q, i)
    }
    if (hasAny(text, STORE_WORDS)) {
        if (q.isEmpty()) {
            return LinkPlan("Play Store mein kya dhoondun?", "پلے اسٹور میں کیا ڈھونڈوں؟", null)
        }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q) + "&c=apps"))
        i.setPackage("com.android.vending")
        return LinkPlan("Play Store mein dhoond raha hoon: " + q, "پلے اسٹور میں ڈھونڈ رہا ہوں: " + q, i)
    }
    if (hasAny(text, INSTA_WORDS)) {
        val name = q.replace(" ", "").trimStart('@').lowercase()
        if (name.isEmpty() || !Regex("^[a-z0-9._]{1,30}$").matches(name)) {
            return LinkPlan(
                "Instagram par sirf account ka username khulta hai. Username angrezi harfon mein bolein (English mode mein).",
                "انسٹاگرام پر صرف اکاؤنٹ کا یوزر نیم کھلتا ہے۔ یوزر نیم انگریزی میں بولیں۔",
                null
            )
        }
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("instagram://user?username=" + name))
        i.setPackage("com.instagram.android")
        return LinkPlan("Instagram par " + name + " ka profile khol raha hoon", "انسٹاگرام پر " + name + " کا پروفائل کھول رہا ہوں", i)
    }
    return null
}

// FILE KHATAM
