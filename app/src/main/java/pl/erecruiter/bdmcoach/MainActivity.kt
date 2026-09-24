package pl.erecruiter.bdmcoach

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var coach: CoachController
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) requestNotificationThenStart() else coach.showError("Do działania potrzebny jest dostęp do mikrofonu.")
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { coach.start() }
    private val updates = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val state = if (Build.VERSION.SDK_INT >= 33) intent.getSerializableExtra(LiveCoachService.EXTRA_STATE, CoachState::class.java)
            else intent.getSerializableExtra(LiveCoachService.EXTRA_STATE) as? CoachState
            state?.let { coach.accept(it) }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); coach = CoachController(this)
        ContextCompat.registerReceiver(this, updates, IntentFilter(LiveCoachService.ACTION_UPDATE), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent { CoachApp(coach, onStart = {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) requestNotificationThenStart()
            else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }) }
    }
    private fun requestNotificationThenStart() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else coach.start()
    }
    override fun onDestroy() { unregisterReceiver(updates); super.onDestroy() }
}

class CoachController(private val activity: ComponentActivity) {
    var running by mutableStateOf(false); private set
    var consent by mutableStateOf(false)
    var backendUrl by mutableStateOf("https://bdm-live-coach.onrender.com")
    var sessionToken by mutableStateOf("12344321")
    var state by mutableStateOf(CoachEngine().analyse("")); private set
    var error by mutableStateOf<String?>(null); private set
    fun start() {
        if (!consent) { showError("Potwierdź, że możesz korzystać z transkrypcji podczas tego spotkania."); return }
        error = null; running = true
        ContextCompat.startForegroundService(activity, Intent(activity, LiveCoachService::class.java).setAction(LiveCoachService.ACTION_START)
            .putExtra(LiveCoachService.EXTRA_BACKEND_URL, backendUrl.trim())
            .putExtra(LiveCoachService.EXTRA_SESSION_TOKEN, sessionToken.trim()))
    }
    fun accept(newState: CoachState) { state = newState; if (newState.status == "Zatrzymano") running = false }
    fun stop() { running = false; activity.startService(Intent(activity, LiveCoachService::class.java).setAction(LiveCoachService.ACTION_STOP)) }
    fun showError(message: String) { error = message; running = false }
}

@Composable
private fun CoachApp(coach: CoachController, onStart: () -> Unit) {
    val navy = Color(0xFF0D1117); val surface = Color(0xFF161B22); val blue = Color(0xFF6EA8FE)
    MaterialTheme(colorScheme = darkColorScheme(primary = blue, surface = surface, background = navy)) {
        Surface(Modifier.fillMaxSize(), color = navy) {
            Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Wersja ${BuildConfig.VERSION_NAME}", color = blue)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text("BDM LIVE COACH", fontWeight = FontWeight.Bold); Text(if (coach.running) "● Słucham — bez zapisu audio" else "Gotowy do spotkania", color = if (coach.running) Color(0xFF6EE7B7) else Color.LightGray, style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = { if (coach.running) coach.stop() else onStart() }, colors = ButtonDefaults.buttonColors(containerColor = if (coach.running) Color(0xFFB42318) else Color(0xFF16A34A))) { Text(if (coach.running) "Zatrzymaj" else "Rozpocznij") }
                }
                HorizontalDivider(color = Color(0xFF30363D))
                Text("STATUS", color = blue, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(coach.state.status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                coach.state.quote?.let { Quote(it) }
                coach.state.cards.forEach { Card(it) }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = coach.consent, onCheckedChange = { coach.consent = it })
                    Text("Mam zgodę na użycie transkrypcji w tym spotkaniu.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
                OutlinedTextField(value = coach.backendUrl, onValueChange = { coach.backendUrl = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Adres serwera AI — opcjonalnie") }, placeholder = { Text("https://coach.twojafirma.pl") })
                if (coach.backendUrl.isNotBlank()) OutlinedTextField(value = coach.sessionToken, onValueChange = { coach.sessionToken = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Token sesji") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                Text("Aplikacja nie zapisuje dźwięku ani pełnej transkrypcji. Sugestie są pomocnicze — potwierdzaj funkcje eRecruitera w aktualnych materiałach.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                coach.error?.let { Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable private fun Quote(text: String) = Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF21262D), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("CYTAT / KONTEKST", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.labelSmall); Text("„${text.take(220)}”", style = MaterialTheme.typography.bodyMedium) } }
@Composable private fun Card(card: CoachCard) = Surface(shape = RoundedCornerShape(14.dp), color = when (card.priority) { Priority.HIGH -> Color(0xFF173451); Priority.MEDIUM -> Color(0xFF202B3B); Priority.LOW -> Color(0xFF1C2128) }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(card.label, color = Color(0xFFA5C8FF), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(card.message, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); Text(card.reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFFC9D1D9)) } }
