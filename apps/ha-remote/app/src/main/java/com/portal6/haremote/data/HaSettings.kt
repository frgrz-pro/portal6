package com.portal6.haremote.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Où est Home Assistant et avec quel jeton. Vide = mode démo (dépôts mockés). */
data class HaSettings(val url: String, val token: String) {
    val isConfigured: Boolean get() = url.isNotBlank() && token.isNotBlank()
    val baseUrl: String get() = url.trim().trimEnd('/')
}

/**
 * Réglages saisis dans l'app (onglet Réglages). Le jeton longue durée HA est une
 * donnée perso : il ne vit que dans le stockage privé de l'app, jamais dans le
 * code ni dans le repo.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("portal6-settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<HaSettings> = _settings

    fun save(url: String, token: String) {
        prefs.edit().putString(KEY_URL, url.trim()).putString(KEY_TOKEN, token.trim()).apply()
        _settings.value = load()
    }

    /** Base du serveur AzuraCast (API publique `/api/nowplaying`, pas de jeton). */
    private val _azuracastUrl = MutableStateFlow(prefs.getString(KEY_AZURACAST, DEFAULT_AZURACAST) ?: DEFAULT_AZURACAST)
    val azuracastUrl: StateFlow<String> = _azuracastUrl

    fun saveAzuracastUrl(url: String) {
        val clean = url.trim().trimEnd('/').ifBlank { DEFAULT_AZURACAST }
        prefs.edit().putString(KEY_AZURACAST, clean).apply()
        _azuracastUrl.value = clean
    }

    /** Clé de **compte** TRMNL (`user_…`, Developer Edition) — pas la clé de device. Vide = démo. */
    private val _trmnlKey = MutableStateFlow(prefs.getString(KEY_TRMNL, "") ?: "")
    val trmnlKey: StateFlow<String> = _trmnlKey

    fun saveTrmnlKey(key: String) {
        val clean = key.trim()
        prefs.edit().putString(KEY_TRMNL, clean).apply()
        _trmnlKey.value = clean
    }

    private fun load() = HaSettings(
        url = prefs.getString(KEY_URL, "") ?: "",
        token = prefs.getString(KEY_TOKEN, "") ?: "",
    )

    private companion object {
        const val KEY_URL = "ha_url"
        const val KEY_TOKEN = "ha_token"
        const val KEY_AZURACAST = "azuracast_url"
        const val KEY_TRMNL = "trmnl_api_key"
        const val DEFAULT_AZURACAST = "http://192.168.0.5"
    }
}
