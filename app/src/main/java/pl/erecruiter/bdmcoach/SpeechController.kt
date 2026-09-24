package pl.erecruiter.bdmcoach

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Handler
import android.os.Looper

class SpeechController(private val context: Context, private val onText: (String, Boolean) -> Unit, private val onError: (String) -> Unit) {
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private val handler = Handler(Looper.getMainLooper())
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
        override fun onError(error: Int) {
            if (!active) return
            onError(errorMessage(error))
            // Recognition services commonly return a transient timeout after a pause.
            // Restart after a short delay, while exposing the actual failure to the UI.
            handler.postDelayed({ if (active) listen() }, 700L)
        }
        override fun onReadyForSpeech(p: Bundle?) {} ; override fun onBeginningOfSpeech() {} ; override fun onRmsChanged(v: Float) {}
        override fun onBufferReceived(b: ByteArray?) {} ; override fun onEndOfSpeech() {} ; override fun onEvent(t: Int, p: Bundle?) {}
    }
    private fun errorMessage(error: Int) = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Nie udało się uzyskać dostępu do mikrofonu."
        SpeechRecognizer.ERROR_CLIENT -> "Usługa rozpoznawania mowy odrzuciła żądanie."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Brak uprawnienia do mikrofonu."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Rozpoznawanie mowy wymaga połączenia z internetem."
        SpeechRecognizer.ERROR_NO_MATCH -> "Nie rozpoznano wypowiedzi — spróbuj mówić wyraźniej."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Rozpoznawanie mowy jest zajęte — ponawiam próbę."
        SpeechRecognizer.ERROR_SERVER -> "Usługa rozpoznawania mowy zwróciła błąd."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nie wykryto mowy — powiedz coś po uruchomieniu nasłuchu."
        else -> "Rozpoznawanie mowy zgłosiło błąd ($error)."
    }

    fun stop() { active = false; handler.removeCallbacksAndMessages(null); recognizer?.cancel(); recognizer?.destroy(); recognizer = null }
}
