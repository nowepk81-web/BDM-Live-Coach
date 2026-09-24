package pl.erecruiter.bdmcoach

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Optional secure relay. The app sends recognised text, never an OpenAI API key. */
class CoachBackend(private val baseUrl: String, private val token: String) {
    private val executor = Executors.newSingleThreadExecutor()
    fun analyse(context: String, onSuccess: (CoachState) -> Unit) = executor.execute {
        try {
            val connection = (URL("${baseUrl.trimEnd('/')}/v1/coach").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; doOutput = true; connectTimeout = 8_000; readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
            }
            connection.outputStream.bufferedWriter().use { it.write(JSONObject().put("context", context).toString()) }
            if (connection.responseCode !in 200..299) return@execute
            val data = connection.inputStream.bufferedReader().use { it.readText() }
            val result = JSONObject(data)
            val cards = result.getJSONArray("cards").let { array ->
                List(array.length()) { index -> array.getJSONObject(index).let { card ->
                    CoachCard(card.getString("label"), card.getString("message"), card.getString("reason"), Priority.valueOf(card.getString("priority")))
                } }
            }
            onSuccess(CoachState(result.getString("status"), if (result.isNull("quote")) null else result.getString("quote"), cards))
        } catch (_: Exception) { /* Local coaching remains available if the relay is unavailable. */ }
    }
}
