package pl.erecruiter.bdmcoach

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Optional secure relay. The app sends recognised text, never an OpenAI API key. */
class CoachBackend(private val baseUrl: String, private val token: String) {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var closed = false
    @Volatile private var busy = false
    fun close() { closed = true; executor.shutdownNow() }
    @Synchronized
    fun analyse(context: String, onSuccess: (CoachState) -> Unit, onError: (String) -> Unit) {
        if (closed || busy) return
        busy = true
        executor.execute {
        var connection: HttpURLConnection? = null
        try {
            val request = (URL("${baseUrl.trimEnd('/')}/v1/coach").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 15_000; readTimeout = 90_000
                instanceFollowRedirects = false
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }
            connection = request
            request.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(JSONObject().put("context", context).toString()) }
            if (request.responseCode !in 200..299) {
                if (!closed) onError(if (request.responseCode == 401) "Token sesji jest nieprawidłowy. Sprawdź COACH_SHARED_SECRET w Render." else "Serwer zwrócił HTTP ${request.responseCode}.")
                return@execute
            }
            val data = request.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val result = JSONObject(data)
            val cards = result.getJSONArray("cards").let { array ->
                List(array.length()) { index -> array.getJSONObject(index).let { card ->
                    CoachCard(card.getString("label"), card.getString("message"), card.getString("reason"), Priority.valueOf(card.getString("priority")))
                } }
            }
            if (!closed) onSuccess(CoachState(result.getString("status"), if (result.isNull("quote")) null else result.getString("quote"), cards))
        } catch (_: Exception) {
            if (!closed) onError("Nie udało się odebrać odpowiedzi AI. Sprawdź internet i serwer; rozpoznawanie mowy działa niezależnie.")
        } finally { connection?.disconnect(); busy = false }
        }
    }
}
