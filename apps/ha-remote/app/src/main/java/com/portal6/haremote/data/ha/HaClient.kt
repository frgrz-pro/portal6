package com.portal6.haremote.data.ha

import android.util.Log
import com.portal6.haremote.data.HaSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Le strict nécessaire de l'API Home Assistant :
 * - REST pour lire les états, appeler un service, lire/écrire une scène ;
 * - WebSocket pour recevoir les changements d'état en temps réel (avec
 *   reconnexion automatique).
 *
 * Auth : jeton longue durée en `Authorization: Bearer`. LAN uniquement.
 */
class HaClient(private val settings: HaSettings) {

    private val rest = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Pas de readTimeout sur le WebSocket (sinon OkHttp coupe après 10 s de silence) ;
    // un ping toutes les 20 s détecte la coupure.
    private val ws = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val _connection = MutableStateFlow("Connexion à Home Assistant…")

    /** Une ligne lisible pour l'UI : connecté / hors ligne / jeton refusé. */
    val connection: StateFlow<String> = _connection

    private fun request(path: String) = Request.Builder()
        .url(settings.baseUrl + path)
        .header("Authorization", "Bearer ${settings.token}")

    private fun Response.bodyOrThrow(): String {
        if (!isSuccessful) throw HaException("HTTP $code sur ${request.url.encodedPath}")
        return body?.string().orEmpty()
    }

    /** `GET /api/` — vérifie URL + jeton. Renvoie le message de HA. */
    suspend fun ping(): String = withContext(Dispatchers.IO) {
        rest.newCall(request("/api/").get().build()).execute().use { r ->
            when (r.code) {
                401 -> throw HaException("Jeton refusé (401)")
                else -> JSONObject(r.bodyOrThrow()).optString("message", "OK")
            }
        }
    }

    /** `GET /api/states` — entityId → état (`on`/`off`/…). */
    suspend fun states(): Map<String, String> = withContext(Dispatchers.IO) {
        rest.newCall(request("/api/states").get().build()).execute().use { r ->
            val array = JSONArray(r.bodyOrThrow())
            buildMap {
                for (i in 0 until array.length()) {
                    val s = array.getJSONObject(i)
                    put(s.getString("entity_id"), s.getString("state"))
                }
            }
        }
    }

    suspend fun callService(domain: String, service: String, data: JSONObject) {
        withContext(Dispatchers.IO) {
            rest.newCall(
                request("/api/services/$domain/$service")
                    .post(data.toString().toRequestBody(jsonType))
                    .build(),
            ).execute().use { it.bodyOrThrow() }
        }
    }

    /** Config d'une scène (`/api/config/scene/config/<id>`), `null` si elle n'existe pas. */
    suspend fun getSceneConfig(id: String): JSONObject? = withContext(Dispatchers.IO) {
        rest.newCall(request("/api/config/scene/config/$id").get().build()).execute().use { r ->
            if (r.code == 404) null else JSONObject(r.bodyOrThrow())
        }
    }

    /** Crée ou remplace une scène de commutateurs : entityId → on/off. */
    suspend fun setSceneConfig(id: String, name: String, entities: Map<String, Boolean>) {
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("id", id)
                .put("name", name)
                .put("entities", JSONObject().apply {
                    entities.forEach { (entityId, on) -> put(entityId, if (on) "on" else "off") }
                })
            rest.newCall(
                request("/api/config/scene/config/$id")
                    .post(body.toString().toRequestBody(jsonType))
                    .build(),
            ).execute().use { it.bodyOrThrow() }
        }
    }

    /**
     * Changements d'état poussés par HA : (entityId, nouvel état). Émet
     * [RESYNC] à chaque (re)connexion réussie pour que l'abonné relise tout.
     * Se reconnecte tout seul tant que le flux est collecté.
     */
    fun stateChanges(): Flow<Pair<String, String>> = callbackFlow {
        var closed = false
        var socket: WebSocket? = null
        // Les deux fonctions locales s'appellent mutuellement : `connect` est
        // déclarée en variable pour être visible avant sa définition.
        var connect: () -> Unit = {}

        fun scheduleReconnect(reason: String) {
            if (closed) return
            _connection.value = "Hors ligne — $reason"
            launch {
                delay(RECONNECT_DELAY_MS)
                if (!closed) connect()
            }
        }

        connect = {
            val url = settings.baseUrl.replaceFirst("http", "ws") + "/api/websocket"
            socket = ws.newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val msg = runCatching { JSONObject(text) }.getOrNull() ?: return
                        when (msg.optString("type")) {
                            "auth_required" -> webSocket.send(
                                JSONObject().put("type", "auth").put("access_token", settings.token).toString(),
                            )
                            "auth_ok" -> {
                                webSocket.send(
                                    JSONObject().put("id", 1).put("type", "subscribe_events")
                                        .put("event_type", "state_changed").toString(),
                                )
                                _connection.value = "Connecté à Home Assistant"
                                trySend(RESYNC to "")
                            }
                            "auth_invalid" -> {
                                _connection.value = "Jeton refusé par Home Assistant"
                                closed = true
                                webSocket.close(1000, "auth")
                            }
                            "event" -> {
                                val data = msg.optJSONObject("event")?.optJSONObject("data") ?: return
                                val newState = data.optJSONObject("new_state") ?: return
                                trySend(data.getString("entity_id") to newState.getString("state"))
                            }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.w(TAG, "WebSocket : ${t.message}")
                        scheduleReconnect(t.message ?: "erreur réseau")
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        scheduleReconnect("fermé ($code)")
                    }
                },
            )
        }

        connect()
        awaitClose {
            closed = true
            socket?.cancel()
        }
    }

    class HaException(message: String) : Exception(message)

    companion object {
        const val RESYNC = "*"
        private const val TAG = "HaClient"
        private const val RECONNECT_DELAY_MS = 3_000L
    }
}
