package com.portal6.haremote.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Persistance locale en `SharedPreferences` + JSON (`org.json`, fourni par le
 * SDK — aucune dépendance ajoutée). Tout ce qui est ici n'est utile qu'au
 * **mode démo** (Home Assistant non configuré) : l'état des prises et les
 * modes 2-4 y survivent à la fermeture de l'app. Avec HA branché, l'état de
 * vérité est celui du backend (prises) et des scènes HA (modes).
 */
class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("portal6-remote", Context.MODE_PRIVATE)

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

    /** Modes 2-4 du mode démo : numéro → état des prises. */
    fun loadModes(): Map<Int, Map<String, Boolean>> {
        val raw = prefs.getString(KEY_MODES, null) ?: return emptyMap()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        return buildMap {
            json.keys().forEach { key ->
                val states = json.optJSONObject(key) ?: return@forEach
                put(key.toInt(), buildMap { states.keys().forEach { e -> put(e, states.optBoolean(e)) } })
            }
        }
    }

    fun saveModes(modes: Map<Int, Map<String, Boolean>>) {
        val json = JSONObject().apply {
            modes.forEach { (n, states) ->
                put(n.toString(), JSONObject().apply { states.forEach { (k, v) -> put(k, v) } })
            }
        }
        prefs.edit().putString(KEY_MODES, json.toString()).apply()
    }

    fun loadMuted(): Boolean = prefs.getBoolean(KEY_MUTED, false)

    fun saveMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MUTED, muted).apply()
    }

    private companion object {
        const val KEY_LIGHTS = "lights_state"
        const val KEY_MODES = "demo_modes"
        const val KEY_MUTED = "tv_muted"
    }
}
