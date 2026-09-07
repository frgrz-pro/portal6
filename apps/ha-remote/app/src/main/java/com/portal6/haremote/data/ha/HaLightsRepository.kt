package com.portal6.haremote.data.ha

import android.util.Log
import com.portal6.haremote.data.DefaultLights
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.LightsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Les prises vues par Home Assistant. L'état de vérité est celui de HA : lu
 * en entier à chaque (re)connexion, puis tenu à jour par le flux WebSocket.
 * Les commandes passent par `switch.turn_on/turn_off/toggle`.
 */
class HaLightsRepository(
    private val client: HaClient,
    scope: CoroutineScope,
    defaults: List<Light> = DefaultLights,
) : LightsRepository {

    private val _lights = MutableStateFlow(defaults)
    override val lights: StateFlow<List<Light>> = _lights
    private val ids = defaults.map { it.entityId }

    init {
        scope.launch {
            client.stateChanges().collect { (entityId, state) ->
                if (entityId == HaClient.RESYNC) {
                    refresh()
                } else if (entityId in ids) {
                    _lights.update { list ->
                        list.map { if (it.entityId == entityId) it.copy(isOn = state == "on") else it }
                    }
                }
            }
        }
    }

    private suspend fun refresh() {
        runCatching { client.states() }
            .onSuccess { states ->
                _lights.update { list -> list.map { it.copy(isOn = states[it.entityId] == "on") } }
            }
            .onFailure { Log.w(TAG, "states : ${it.message}") }
    }

    override suspend fun toggle(entityId: String) = call("toggle", listOf(entityId))

    override suspend fun setAll(on: Boolean) = call(if (on) "turn_on" else "turn_off", ids)

    override suspend fun apply(states: Map<String, Boolean>) {
        val on = states.filterValues { it }.keys.toList()
        val off = states.filterValues { !it }.keys.toList()
        if (on.isNotEmpty()) call("turn_on", on)
        if (off.isNotEmpty()) call("turn_off", off)
    }

    private suspend fun call(service: String, entityIds: List<String>) {
        runCatching {
            client.callService("switch", service, JSONObject().put("entity_id", JSONArray(entityIds)))
        }.onFailure { Log.w(TAG, "switch.$service : ${it.message}") }
    }

    private companion object {
        const val TAG = "HaLights"
    }
}
