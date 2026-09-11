package com.portal6.haremote.data.ha

import android.util.Log
import com.portal6.haremote.data.HaSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import java.io.IOException
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

    private val wakeups = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Rouvre le WebSocket tout de suite, sans attendre qu'OkHttp constate la
     * coupure (ping 20 s + pong manquant = jusqu'à 40 s). À appeler au retour
     * au premier plan : après une mise en veille, le socket est souvent un
     * zombie qui affiche encore « Connecté ». La (re)connexion relit tous les
     * états ([RESYNC]).
     */
    fun reconnect() {
        wakeups.tryEmit(Unit)
    }

    private fun request(path: String) = Request.Builder()
        .url(settings.baseUrl + path)
        .header("Authorization", "Bearer ${settings.token}")

    private fun Response.bodyOrThrow(): String {
        if (!isSuccessful) throw HaException("HTTP $code sur ${request.url.encodedPath}")
        return body?.string().orEmpty()
    }

    /**
     * Tout appel REST passe par là : une erreur réseau (HA injoignable, Wi-Fi
     * pas encore revenu) se voit dans [connection] et relance le WebSocket, au
     * lieu de mourir en silence dans un `Log.w`.
     */
    private suspend fun <T> restCall(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: IOException) {
            _connection.value = "Hors ligne — ${e.message ?: "erreur réseau"}"
            reconnect()
            throw e
        }
    }

    /** `GET /api/` — vérifie URL + jeton. Renvoie le message de HA. */
    suspend fun ping(): String = restCall {
        rest.newCall(request("/api/").get().build()).execute().use { r ->
            when (r.code) {
                401 -> throw HaException("Jeton refusé (401)")
                else -> JSONObject(r.bodyOrThrow()).optString("message", "OK")
            }
        }
    }

    /** `GET /api/states` — entityId → état (`on`/`off`/…). */
    suspend fun states(): Map<String, String> = restCall {
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
        restCall {
            rest.newCall(
                request("/api/services/$domain/$service")
                    .post(data.toString().toRequestBody(jsonType))
                    .build(),
            ).execute().use { it.bodyOrThrow() }
        }
    }

    /** Config d'une scène (`/api/config/scene/config/<id>`), `null` si elle n'existe pas. */
    suspend fun getSceneConfig(id: String): JSONObject? = restCall {
        rest.newCall(request("/api/config/scene/config/$id").get().build()).execute().use { r ->
            if (r.code == 404) null else JSONObject(r.bodyOrThrow())
        }
    }

    /** Crée ou remplace une scène de commutateurs : entityId → on/off. */
    suspend fun setSceneConfig(id: String, name: String, entities: Map<String, Boolean>) {
        restCall {
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
     * Se reconnecte tout seul tant que le flux est collecté : après une
     * coupure (3 s de délai), et immédiatement sur [reconnect].
     */
    fun stateChanges(): Flow<Pair<String, String>> = callbackFlow {
        var closed = false
        var socket: WebSocket? = null
        // Numéro de la connexion courante : les callbacks d'un ancien socket
        // (annulé par une reconnexion forcée) sont ignorés grâce à lui.
        var generation = 0
        // Les deux fonctions locales s'appellent mutuellement : `connect` est
        // déclarée en variable pour être visible avant sa définition.
        var connect: () -> Unit = {}

        fun scheduleReconnect(gen: Int, reason: String) {
            if (closed || gen != generation) return
            _connection.value = "Hors ligne — $reason"
            launch {
                delay(RECONNECT_DELAY_MS)
                if (!closed && gen == generation) connect()
            }
        }

        connect = {
            val gen = ++generation
            socket?.cancel()
            val url = settings.baseUrl.replaceFirst("http", "ws") + "/api/websocket"
            socket = ws.newWebSocket(
                Request.Builder().url(url).build(),
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (gen != generation) return
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
                        if (gen != generation) return
                        Log.w(TAG, "WebSocket : ${t.message}")
                        scheduleReconnect(gen, t.message ?: "erreur réseau")
                    }

                    // HA ferme de son côté (client endormi trop longtemps, redémarrage) :
                    // il faut répondre au close, sinon OkHttp n'appelle jamais onClosed
                    // et le socket reste un zombie jusqu'au prochain ping raté.
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(code, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (gen != generation) return
                        scheduleReconnect(gen, "fermé ($code)")
                    }
                },
            )
        }

        connect()
        launch {
            wakeups.collect {
                if (closed) return@collect
                _connection.value = "Reconnexion à Home Assistant…"
                connect()
            }
        }
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
