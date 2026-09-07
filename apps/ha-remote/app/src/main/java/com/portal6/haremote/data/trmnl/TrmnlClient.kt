package com.portal6.haremote.data.trmnl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Un TRMNL du compte. `refreshInterval` en secondes (300 à 86 400). */
data class TrmnlDevice(
    val id: Int,
    val name: String,
    val friendlyId: String,
    val batteryPercent: Int,
    val refreshInterval: Int,
    val lastPingAt: String?,
)

/** Une instance de plugin du compte (« plugin setting » chez TRMNL). */
data class TrmnlPlugin(
    val id: Int,
    val name: String,
    /** `polling` / `webhook` pour un private plugin, `null` pour un plugin du catalogue. */
    val strategy: String?,
)

/** Une ligne de la playlist d'un device. */
data class TrmnlPlaylistItem(
    val id: Int,
    val pluginSettingId: Int?,
    val visible: Boolean,
    val rowOrder: Int,
    val renderedAt: String?,
)

/**
 * Le strict nécessaire de l'API **compte** TRMNL (clé `user_…`, Developer
 * Edition) : devices, instances de plugins, playlist d'un device. Spec :
 * https://trmnl.com/api-docs/openapi.yaml. Ne pas confondre avec la clé de
 * device (`Access-Token`) qui sert à lire l'écran.
 *
 * Pas d'endpoint « afficher maintenant » : le device tire son image à chaque
 * check-in, donc tout changement n'est visible qu'au prochain.
 */
class TrmnlClient(private val apiKey: String) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun request(path: String) = Request.Builder()
        .url(BASE_URL + path)
        .header("Authorization", "Bearer ${apiKey.trim()}")

    private fun Response.bodyOrThrow(): String {
        when (code) {
            401 -> throw TrmnlException("Clé refusée (401)")
            404 -> throw TrmnlException("Introuvable (404) sur ${request.url.encodedPath}")
        }
        if (!isSuccessful) throw TrmnlException("HTTP $code sur ${request.url.encodedPath}")
        return body?.string().orEmpty()
    }

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        http.newCall(request(path).get().build()).execute().use { JSONObject(it.bodyOrThrow()) }
    }

    private suspend fun send(method: String, path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        http.newCall(request(path).method(method, body.toString().toRequestBody(jsonType)).build())
            .execute().use { r -> r.bodyOrThrow().let { if (it.isBlank()) JSONObject() else JSONObject(it) } }
    }

    private fun JSONObject.data(): JSONArray = optJSONArray("data") ?: JSONArray()

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    /** `GET /api/me` — vérifie la clé. Renvoie l'e-mail du compte. */
    suspend fun ping(): String = get("/api/me").optJSONObject("data")?.optString("email", "OK") ?: "OK"

    suspend fun devices(): List<TrmnlDevice> = get("/api/devices").data().let { a ->
        List(a.length()) { i ->
            val d = a.getJSONObject(i)
            TrmnlDevice(
                id = d.getInt("id"),
                name = d.optString("name"),
                friendlyId = d.optString("friendly_id"),
                batteryPercent = d.optDouble("percent_charged", 0.0).toInt(),
                refreshInterval = d.optInt("refresh_interval", 900),
                lastPingAt = d.stringOrNull("last_ping_at"),
            )
        }
    }

    suspend fun pluginSettings(): List<TrmnlPlugin> = get("/api/plugin_settings").data().let { a ->
        List(a.length()) { i ->
            val p = a.getJSONObject(i)
            TrmnlPlugin(
                id = p.getInt("id"),
                name = p.optString("name"),
                strategy = p.stringOrNull("strategy"),
            )
        }
    }

    suspend fun playlist(deviceId: Int): List<TrmnlPlaylistItem> =
        get("/api/devices/$deviceId/playlist_items").data().let { a ->
            List(a.length()) { i -> a.getJSONObject(i).toItem() }.sortedBy { it.rowOrder }
        }

    private fun JSONObject.toItem() = TrmnlPlaylistItem(
        id = getInt("id"),
        pluginSettingId = if (isNull("plugin_setting_id")) null else getInt("plugin_setting_id"),
        visible = optBoolean("visible", true),
        rowOrder = optInt("row_order", 0),
        renderedAt = stringOrNull("rendered_at"),
    )

    suspend fun addToPlaylist(deviceId: Int, pluginSettingId: Int): TrmnlPlaylistItem =
        send(
            "POST",
            "/api/devices/$deviceId/playlist_items",
            JSONObject().put("plugin_setting_id", pluginSettingId.toString()),
        ).getJSONObject("data").toItem()

    suspend fun setVisible(itemId: Int, visible: Boolean) {
        send("PATCH", "/api/playlists/items/$itemId", JSONObject().put("visible", visible))
    }

    /** Tous les ids de la playlist, dans le nouvel ordre (TRMNL refuse une liste partielle). */
    suspend fun reorder(deviceId: Int, itemIds: List<Int>) {
        send("PUT", "/api/devices/$deviceId/playlist_items/order", JSONObject().put("playlist_item_ids", JSONArray(itemIds)))
    }

    suspend fun setRefreshInterval(deviceId: Int, seconds: Int) {
        send("PATCH", "/api/devices/$deviceId", JSONObject().put("refresh_interval", seconds))
    }

    class TrmnlException(message: String) : Exception(message)

    companion object {
        const val BASE_URL = "https://trmnl.com"
    }
}
