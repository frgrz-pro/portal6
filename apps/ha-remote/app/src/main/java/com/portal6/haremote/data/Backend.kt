package com.portal6.haremote.data

import com.portal6.haremote.data.ha.HaClient
import com.portal6.haremote.data.ha.HaLightsRepository
import com.portal6.haremote.data.ha.HaModesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Un backend = un jeu de dépôts cohérent : Home Assistant quand les réglages
 * sont remplis, mock sinon. Il est reconstruit à chaque changement de
 * réglages ; les dépôts « délégués » exposés à l'UI et aux tuiles, eux, ne
 * changent jamais d'identité — ils suivent le backend courant.
 */
class Backend(
    val lights: LightsRepository,
    val modes: ModesRepository,
    val client: HaClient?,
    private val scope: CoroutineScope,
) {
    fun close() = scope.cancel()
}

class BackendHolder(
    settingsStore: SettingsStore,
    private val store: ConfigStore,
    appScope: CoroutineScope,
) {
    private val _backend = MutableStateFlow(build(settingsStore.settings.value))
    val backend: StateFlow<Backend> = _backend

    @OptIn(ExperimentalCoroutinesApi::class)
    val connection: StateFlow<String> = _backend
        .flatMapLatest { it.client?.connection ?: flowOf(DEMO_STATUS) }
        .stateIn(appScope, SharingStarted.Eagerly, DEMO_STATUS)

    init {
        appScope.launch {
            settingsStore.settings.drop(1).collect { settings ->
                _backend.value.close()
                _backend.value = build(settings)
            }
        }
    }

    private fun build(settings: HaSettings): Backend {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        return if (settings.isConfigured) {
            val client = HaClient(settings)
            val lights = HaLightsRepository(client, scope)
            Backend(lights, HaModesRepository(client, lights, scope), client, scope)
        } else {
            val lights = MockLightsRepository(store)
            Backend(lights, MockModesRepository(store, lights), null, scope)
        }
    }

    companion object {
        const val DEMO_STATUS = "Mode démo — Home Assistant non configuré (onglet Réglages)"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DelegatingLightsRepository(
    private val holder: BackendHolder,
    scope: CoroutineScope,
) : LightsRepository {
    override val lights: StateFlow<List<Light>> = holder.backend
        .flatMapLatest { it.lights.lights }
        .stateIn(scope, SharingStarted.Eagerly, holder.backend.value.lights.lights.value)

    override suspend fun toggle(entityId: String) = holder.backend.value.lights.toggle(entityId)
    override suspend fun setAll(on: Boolean) = holder.backend.value.lights.setAll(on)
    override suspend fun apply(states: Map<String, Boolean>) = holder.backend.value.lights.apply(states)
}

@OptIn(ExperimentalCoroutinesApi::class)
class DelegatingModesRepository(
    private val holder: BackendHolder,
    scope: CoroutineScope,
) : ModesRepository {
    override val modes: StateFlow<List<Mode>> = holder.backend
        .flatMapLatest { it.modes.modes }
        .stateIn(scope, SharingStarted.Eagerly, holder.backend.value.modes.modes.value)

    override suspend fun apply(mode: Mode) = holder.backend.value.modes.apply(mode)
    override suspend fun save(number: Int, states: Map<String, Boolean>) =
        holder.backend.value.modes.save(number, states)
}
