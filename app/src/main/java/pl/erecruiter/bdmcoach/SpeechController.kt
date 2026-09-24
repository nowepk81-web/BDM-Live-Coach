package pl.erecruiter.bdmcoach

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SpeechController(private val context: Context, private val onText: (String, Boolean) -> Unit, private val onError: (String) -> Unit) {
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) { onError("Rozpoznawanie mowy nie jest dostępne na tym urządzeniu."); return }
        active = true; recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { it.setRecognitionListener(listener); listen() }
    }
    private fun listen() {
        if (!active) return
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL"); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        })
    }
    private val listener = object : RecognitionListener {
        override fun onPartialResults(b: Bundle?) { b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onText(it, false) } }
        override fun onResults(b: Bundle?) { b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onText(it, true) }; listen() }
        override fun onError(error: Int) { if (active) listen() }
        override fun onReadyForSpeech(p: Bundle?) {} ; override fun onBeginningOfSpeech() {} ; override fun onRmsChanged(v: Float) {}
        override fun onBufferReceived(b: ByteArray?) {} ; override fun onEndOfSpeech() {} ; override fun onEvent(t: Int, p: Bundle?) {}
    }
    fun stop() { active = false; recognizer?.cancel(); recognizer?.destroy(); recognizer = null }
}
