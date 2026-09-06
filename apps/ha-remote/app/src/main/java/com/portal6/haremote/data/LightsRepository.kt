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
}

class MockLightsRepository(
    initial: List<Light> = DefaultLights,
) : LightsRepository {

    private val _lights = MutableStateFlow(initial)
    override val lights: StateFlow<List<Light>> = _lights

    override suspend fun toggle(entityId: String) {
        _lights.value = _lights.value.map {
            if (it.entityId == entityId) it.copy(isOn = !it.isOn) else it
        }
    }

    override suspend fun setAll(on: Boolean) {
        _lights.value = _lights.value.map { it.copy(isOn = on) }
    }
}
