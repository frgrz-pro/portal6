package com.portal6.haremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portal6.haremote.Portal6App
import com.portal6.haremote.data.ConfigStore
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.LightsRepository
import com.portal6.haremote.data.RoomConfig
import com.portal6.haremote.data.Rooms
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LightsViewModel(
    private val repository: LightsRepository,
    private val store: ConfigStore,
) : ViewModel() {

    private val room = Rooms.Salon

    val lights: StateFlow<List<Light>> = repository.lights

    /** Les configs enregistrées pour la pièce, dans leur ordre de création. */
    val configs: StateFlow<List<RoomConfig>> = store.configs
        .map { all -> all.filter { it.roomId == room.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), store.configsOf(room.id))

    val activeConfigId: StateFlow<String?> = store.activeConfigId

    fun toggle(entityId: String) {
        viewModelScope.launch {
            repository.toggle(entityId)
            // L'état ne correspond plus à la config appliquée.
            store.setActiveConfig(null)
        }
    }

    fun setAll(on: Boolean) {
        viewModelScope.launch {
            repository.setAll(on)
            store.setActiveConfig(null)
        }
    }

    /** Enregistre l'état courant du salon sous [name] (écrase si le nom existe). */
    fun saveConfig(name: String) {
        if (name.isBlank()) return
        val config = store.saveConfig(name, room, lights.value)
        store.setActiveConfig(config.id)
    }

    fun applyConfig(config: RoomConfig) {
        viewModelScope.launch {
            repository.apply(config.states)
            store.setActiveConfig(config.id)
        }
    }

    fun deleteConfig(id: String) = store.deleteConfig(id)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Portal6App
                LightsViewModel(app.container.lights, app.container.store)
            }
        }
    }
}
