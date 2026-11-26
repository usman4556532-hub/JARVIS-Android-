package com.jarvis.ai

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout

// Handles the whole "circle to search" flow: screenshot -> circle -> Gemini -> answer.
class ScreenSearch(private val context: Context, private val screen: DualScreen) {

    var shot: Bitmap? = null
    var query: String = ""
    private val vision = GeminiVision()
    private var overlay: CircleView? = null

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
        screen.thinking("Cheez ko ungli se gherein", "Screen")
        val v = CircleView(context, s) { crop ->
            root.post {
                overlay?.let { root.removeView(it) }
                overlay = null
            }
            analyse(crop, onResult)
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

    private fun analyse(crop: Bitmap, onResult: (Boolean) -> Unit) {
        screen.thinking("", "Dekh raha hoon…")
        Thread {
            var result: VisionResult? = null
            var problem = ""
            try {
                result = vision.look(crop)
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
}

// FILE KHATAM
