package com.portal6.haremote.data.ha

import android.util.Log
import com.portal6.haremote.data.LightsRepository
import com.portal6.haremote.data.Mode
import com.portal6.haremote.data.ModesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Modes 2-4 = scènes Home Assistant `scene.mode_2..4` (objet `mode_n` de
 * l'API config). C'est ce qui permet aux boutons MOES (automatisations HA)
 * et à l'app de jouer exactement la même chose. Le mode 1 n'a pas de scène :
 * il se calcule sur l'état courant des prises.
 */
class HaModesRepository(
    private val client: HaClient,
    private val lights: LightsRepository,
    scope: CoroutineScope,
) : ModesRepository {

    private val _modes = MutableStateFlow(Mode.defaults())
    override val modes: StateFlow<List<Mode>> = _modes

    init {
        scope.launch { refresh() }
    }

    suspend fun refresh() {
        val list = Mode.defaults().map { mode ->
            if (!mode.isEditable) return@map mode
            val config = runCatching { client.getSceneConfig(mode.id) }
                .onFailure { Log.w(TAG, "scène ${mode.id} : ${it.message}") }
                .getOrNull()
            val entities = config?.optJSONObject("entities")
            mode.copy(states = entities?.let { e ->
                buildMap { e.keys().forEach { k -> put(k, e.optString(k) == "on") } }
            })
        }
        _modes.value = list
    }

    override suspend fun apply(mode: Mode) {
        if (mode.isAllToggle) {
            lights.setAll(!lights.lights.value.any { it.isOn })
            return
        }
        runCatching {
            client.callService("scene", "turn_on", JSONObject().put("entity_id", "scene.${mode.id}"))
        }.onFailure { Log.w(TAG, "scene.turn_on ${mode.id} : ${it.message}") }
    }

    override suspend fun save(number: Int, states: Map<String, Boolean>) {
        val mode = Mode(number, states)
        runCatching { client.setSceneConfig(mode.id, mode.label, states) }
            .onSuccess { refresh() }
            .onFailure { Log.w(TAG, "écriture ${mode.id} : ${it.message}") }
    }

    private companion object {
        const val TAG = "HaModes"
    }
}
