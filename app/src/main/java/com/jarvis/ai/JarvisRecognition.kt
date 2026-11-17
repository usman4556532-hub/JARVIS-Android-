package com.jarvis.ai

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

// Placeholder. Android wants every assistant app to have one of these.
// It does not listen yet. Real listening comes in Marhala 2.
class JarvisRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }
    override fun onCancel(listener: Callback?) {}
    override fun onStopListening(listener: Callback?) {}
}
