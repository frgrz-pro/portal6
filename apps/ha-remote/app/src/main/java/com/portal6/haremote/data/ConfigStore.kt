package com.portal6.haremote.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.util.UUID

/**
 * Persistance locale, en `SharedPreferences` + JSON (`org.json`, fourni par
 * l'SDK — aucune dépendance ajoutée). Le volume est minuscule : 8 booléens et
 * une poignée de configs nommées.
 *
 * Deux choses distinctes sont stockées :
 * - les **configs de pièce** ([configs]) : une feature applicative, qui reste
 *   valable quand le vrai client Home Assistant remplacera le mock ;
 * - l'**état courant des prises** ([loadLights] / [saveLights]) : béquille du
 *   mock uniquement. Avec HA branché, l'état vient du backend, pas du disque.
 */
class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("portal6-remote", Context.MODE_PRIVATE)

    private val _configs = MutableStateFlow(
        RoomConfig.listFromJson(prefs.getString(KEY_CONFIGS, null)),
    )
    val configs: StateFlow<List<RoomConfig>> = _configs

    private val _activeConfigId = MutableStateFlow(prefs.getString(KEY_ACTIVE, null))

    /** Id de la dernière config appliquée, pour que la tuile sache où elle en est. */
    val activeConfigId: StateFlow<String?> = _activeConfigId

    fun configsOf(roomId: String): List<RoomConfig> = _configs.value.filter { it.roomId == roomId }

    /** Enregistre l'état courant de la pièce sous [name]. Écrase la config de même nom. */
    fun saveConfig(name: String, room: Room, lights: List<Light>): RoomConfig {
        val states = lights.filter { it.entityId in room.entityIds }
            .associate { it.entityId to it.isOn }
        val existing = _configs.value.firstOrNull {
            it.roomId == room.id && it.name.equals(name, ignoreCase = true)
        }
        val config = RoomConfig(
            id = existing?.id ?: UUID.randomUUID().toString(),
            roomId = room.id,
            name = name.trim(),
            states = states,
        )
        _configs.value = _configs.value.filterNot { it.id == config.id } + config
        persistConfigs()
        return config
    }

    fun deleteConfig(id: String) {
        _configs.value = _configs.value.filterNot { it.id == id }
        if (_activeConfigId.value == id) setActiveConfig(null)
        persistConfigs()
    }

    fun setActiveConfig(id: String?) {
        _activeConfigId.value = id
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    /**
     * Config suivante dans la liste de la pièce, en boucle. `null` si la pièce
     * n'a aucune config enregistrée.
     */
    fun nextConfig(roomId: String): RoomConfig? {
        val roomConfigs = configsOf(roomId)
        if (roomConfigs.isEmpty()) return null
        val index = roomConfigs.indexOfFirst { it.id == _activeConfigId.value }
        return roomConfigs[(index + 1) % roomConfigs.size]
    }

    fun loadLights(defaults: List<Light>): List<Light> {
        val raw = prefs.getString(KEY_LIGHTS, null) ?: return defaults
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return defaults
        return defaults.map { light ->
            if (json.has(light.entityId)) light.copy(isOn = json.optBoolean(light.entityId)) else light
        }
    }

    fun saveLights(lights: List<Light>) {
        val json = JSONObject().apply { lights.forEach { put(it.entityId, it.isOn) } }
        prefs.edit().putString(KEY_LIGHTS, json.toString()).apply()
    }

    fun loadMuted(): Boolean = prefs.getBoolean(KEY_MUTED, false)

    fun saveMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    private fun persistConfigs() {
        prefs.edit().putString(KEY_CONFIGS, RoomConfig.listToJson(_configs.value)).apply()
    }

    private companion object {
        const val KEY_CONFIGS = "room_configs"
        const val KEY_ACTIVE = "active_config_id"
        const val KEY_LIGHTS = "lights_state"
        const val KEY_MUTED = "tv_muted"
    }
}
