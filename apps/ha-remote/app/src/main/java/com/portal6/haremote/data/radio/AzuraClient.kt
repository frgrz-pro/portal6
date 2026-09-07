package com.portal6.haremote.data.radio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Une station AzuraCast telle que vue par `/api/nowplaying`, avec ce qui passe à l'antenne. */
data class Station(
    val id: Int,
    val shortcode: String,
    val name: String,
    /** URL du flux par défaut (mount `is_default`, sinon `listen_url`). */
    val streamUrl: String,
    val artist: String,
    val title: String,
    val artUrl: String?,
    val isLive: Boolean,
    val streamer: String,
    val listeners: Int,
) {
    val nowPlaying: String
        get() = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" — ")
}

/**
 * Le strict nécessaire de l'API publique AzuraCast : `GET /api/nowplaying` liste
 * toutes les stations publiques et leur titre en cours — sans authentification.
 * Le mini player ne fait que lire ; la lecture elle-même passe par ExoPlayer
 * sur `streamUrl`.
 */
class AzuraClient(baseUrl: String) {

    private val base = baseUrl.trim().trimEnd('/')

    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun nowPlaying(): List<Station> = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url("$base/api/nowplaying").get().build()).execute().use { r ->
            if (!r.isSuccessful) throw AzuraException("HTTP ${r.code} sur /api/nowplaying")
            parse(JSONArray(r.body?.string().orEmpty()))
        }
    }

    /** Pochette : téléchargée à la main, on n'embarque pas de lib d'images pour une vignette. */
    suspend fun art(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url(url).get().build()).execute().use { r ->
                if (!r.isSuccessful) {
                    null
                } else {
                    r.body?.bytes()?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                }
            }
        }.getOrNull()
    }

    private fun parse(array: JSONArray): List<Station> = buildList {
        for (i in 0 until array.length()) {
            val entry = array.getJSONObject(i)
            val station = entry.getJSONObject("station")
            val song = entry.optJSONObject("now_playing")?.optJSONObject("song")
            val live = entry.optJSONObject("live")
            add(
                Station(
                    id = station.getInt("id"),
                    shortcode = station.optString("shortcode"),
                    name = station.optString("name"),
                    streamUrl = defaultMount(station) ?: station.optString("listen_url"),
                    artist = song?.optString("artist").orEmpty(),
                    title = song?.optString("title").orEmpty(),
                    artUrl = song?.optString("art")?.takeIf { it.isNotBlank() },
                    isLive = live?.optBoolean("is_live") ?: false,
                    streamer = live?.optString("streamer_name").orEmpty(),
                    listeners = entry.optJSONObject("listeners")?.optInt("current") ?: 0,
                ),
            )
        }
    }

    private fun defaultMount(station: JSONObject): String? {
        val mounts = station.optJSONArray("mounts") ?: return null
        for (i in 0 until mounts.length()) {
            val m = mounts.getJSONObject(i)
            if (m.optBoolean("is_default")) return m.optString("url").takeIf { it.isNotBlank() }
        }
        return null
    }
}

class AzuraException(message: String) : Exception(message)
