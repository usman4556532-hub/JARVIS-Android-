package com.jarvis.ai

import android.graphics.Color

val AQUA = Color.parseColor("#22D3EE")
val AMBER = Color.parseColor("#FBBF24")
val VIOLET = Color.parseColor("#8B5CF6")
val SOFT_WHITE = Color.parseColor("#F1F2FF")

val NEWS_WORDS = listOf("news", "khabar", "خبر", "نیوز", "headline", "ہیڈ لائن", "ہیڈلائن")
val ALARM_WORDS = listOf("alarm", "الارم", "الارام", "ایلارم", "جگا")
val SEARCH_WORDS = listOf("search", "سرچ", "تلاش", "ڈھونڈ", "dhoond", "dhund")
val DATE_WORDS = listOf("date", "تاریخ", "tareekh", "tarikh")
val TIME_WORDS = listOf("time", "ٹائم", "وقت", "waqt", "baje", "بجے")
val OPEN_WORDS = listOf("open", "kholo", "khol", "کھول", "اوپن", "chalao", "چلاؤ")

val BAJE_WORDS = setOf("baje", "بجے", "bajay", "بجکر")
val AM_WORDS = setOf("am", "صبح", "subah")
val PM_WORDS = setOf("pm", "شام", "sham", "shaam", "raat", "رات", "dopahar", "دوپہر")

val NUMBER_WORDS = mapOf(
    "ایک" to 1, "تین" to 3, "چار" to 4, "پانچ" to 5, "چھ" to 6, "سات" to 7,
    "آٹھ" to 8, "نو" to 9, "دس" to 10, "گیارہ" to 11, "بارہ" to 12,
    "ek" to 1, "teen" to 3, "char" to 4, "panch" to 5, "chhe" to 6, "che" to 6,
    "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10, "gyarah" to 11, "barah" to 12
)

val APP_ALIASES = mapOf(
    "instagram" to listOf("انسٹاگرام", "انسٹا گرام", "insta"),
    "youtube" to listOf("یوٹیوب", "یو ٹیوب", "you tube"),
    "whatsapp" to listOf("واٹس ایپ", "واٹس اپ", "واٹسایپ", "واٹساپ", "whats app"),
    "chrome" to listOf("کروم"),
    "spotify" to listOf("سپوٹیفائی", "اسپاٹیفائی"),
    "camera" to listOf("کیمرہ", "کیمرا"),
    "settings" to listOf("سیٹنگ", "سیٹنگز"),
    "facebook" to listOf("فیس بک", "فیسبک"),
    "photos" to listOf("فوٹوز", "تصویریں"),
    "gallery" to listOf("گیلری"),
    "calculator" to listOf("کیلکولیٹر"),
    "clock" to listOf("گھڑی", "کلاک"),
    "phone" to listOf("فون"),
    "maps" to listOf("میپس", "نقشہ"),
    "play store" to listOf("پلے اسٹور", "پلے سٹور"),
    "gmail" to listOf("جی میل", "جیمیل"),
    "telegram" to listOf("ٹیلیگرام", "ٹیلی گرام")
)

// FILE KHATAM
