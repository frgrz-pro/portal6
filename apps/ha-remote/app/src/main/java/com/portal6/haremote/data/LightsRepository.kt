package com.portal6.haremote.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Frontière entre l'UI et le backend. L'implémentation réelle parlera à
 * Home Assistant (REST pour les commandes, WebSocket pour l'état) ; en
 * attendant que la tour + le coordinateur Zigbee existent, [MockLightsRepository]
 * simule tout en mémoire.
 */
interface LightsRepository {
    val lights: StateFlow<List<Light>>
    suspend fun toggle(entityId: String)
    suspend fun setAll(on: Boolean)

    /** Pousse un jeu d'états d'un coup — c'est ce que fait « appliquer une config ». */
    suspend fun apply(states: Map<String, Boolean>)
}

/**
 * Mock en mémoire, adossé à [ConfigStore] pour que l'état survive à la
 * fermeture de l'app (et soit partagé avec les tuiles des réglages rapides,
 * qui tournent dans le même process). Cette persistance est une béquille du
 * mock : avec HA branché, l'état de vérité est celui du backend.
 */
class MockLightsRepository(
    private val store: ConfigStore,
    defaults: List<Light> = DefaultLights,
) : LightsRepository {

    private val _lights = MutableStateFlow(store.loadLights(defaults))
    override val lights: StateFlow<List<Light>> = _lights

    override suspend fun toggle(entityId: String) {
        update { list -> list.map { if (it.entityId == entityId) it.copy(isOn = !it.isOn) else it } }
    }

    override suspend fun setAll(on: Boolean) {
        update { list -> list.map { it.copy(isOn = on) } }
    }

    override suspend fun apply(states: Map<String, Boolean>) {
        update { list ->
            list.map { light -> states[light.entityId]?.let { light.copy(isOn = it) } ?: light }
        }
    }

    private fun update(transform: (List<Light>) -> List<Light>) {
        _lights.value = transform(_lights.value).also(store::saveLights)
    }
}
