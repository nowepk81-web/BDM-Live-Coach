package pl.erecruiter.bdmcoach

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A visible Android foreground service. Its notification and the OS microphone
 * indicator are intentional: a meeting assistant must never record covertly.
 */
class LiveCoachService : Service() {
    private val engine = CoachEngine()
    private var speech: SpeechController? = null
    private var backend: CoachBackend? = null
    private var history = ""
    private var lastAnalysisAt = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopLiveCoach()
            ACTION_START -> startLiveCoach(intent.getStringExtra(EXTRA_BACKEND_URL).orEmpty(), intent.getStringExtra(EXTRA_SESSION_TOKEN).orEmpty())
        }
        return START_NOT_STICKY
    }

    private fun startLiveCoach(backendUrl: String, sessionToken: String) {
        createChannel()
        startForeground(NOTIFICATION_ID, notification())
        speech?.stop()
        backend?.close()
        history = ""
        lastAnalysisAt = 0L
        backend = backendUrl.takeIf { it.startsWith("https://") }?.let { CoachBackend(it, sessionToken) }
        sendStatus("Nasłuchuję spotkania", null)
        speech = SpeechController(this, ::onSpeech, ::onError).also { it.start() }
    }

    private fun onSpeech(text: String, isFinal: Boolean) {
        // Show immediate feedback even before a final utterance is delivered.
        // This makes microphone/recognizer failures distinguishable from an
        // empty coaching result.
        if (!isFinal) sendStatus("Słyszę: ${text.take(120)}", null)
        val now = System.currentTimeMillis()
        if (isFinal) history = (history + " " + text).takeLast(8_000)
        // Avoid a visually noisy stream of cards, but refresh at a useful pace.
        if (isFinal || now - lastAnalysisAt >= 2_500) {
            lastAnalysisAt = now
            val context = if (isFinal) history else "$history $text"
            publish(engine.analyse(context).copy(quote = text.take(280))) // immediate local fallback
            if (isFinal) backend?.analyse(context, ::publish) { message -> sendStatus("Błąd połączenia AI", message) }
        }
    }

    private fun onError(message: String) { sendStatus("Problem rozpoznawania mowy", message) }
    private fun publish(state: CoachState) = sendBroadcast(Intent(ACTION_UPDATE).setPackage(packageName).putExtra(EXTRA_STATE, state))
    private fun sendStatus(status: String, detail: String?) = publish(CoachState(status, detail, emptyList()))

    private fun stopLiveCoach() {
        speech?.stop(); speech = null
        backend?.close(); backend = null
        sendStatus("Zatrzymano", null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() { speech?.stop(); speech = null; backend?.close(); backend = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Aktywne spotkanie", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Informuje, gdy BDM Live Coach używa mikrofonu."
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
    }

    private fun notification(): Notification {
        val openApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, LiveCoachService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("BDM Live Coach działa")
            .setContentText("Mikrofon aktywny — dotknij, aby zobaczyć sugestie.")
            .setSilent(true).setOngoing(true).setContentIntent(openApp)
            .addAction(0, "Zatrzymaj", stop).build()
    }

    companion object {
        const val ACTION_START = "pl.erecruiter.bdmcoach.START"
        const val ACTION_STOP = "pl.erecruiter.bdmcoach.STOP"
        const val ACTION_UPDATE = "pl.erecruiter.bdmcoach.UPDATE"
        const val EXTRA_STATE = "coach_state"
        const val EXTRA_BACKEND_URL = "backend_url"
        const val EXTRA_SESSION_TOKEN = "session_token"
        private const val CHANNEL_ID = "live_coach_meeting"
        private const val NOTIFICATION_ID = 701
    }
}
