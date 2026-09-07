package com.portal6.haremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portal6.haremote.Portal6App
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.LightsRepository
import com.portal6.haremote.data.Mode
import com.portal6.haremote.data.ModesRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LightsViewModel(
    private val repository: LightsRepository,
    private val modesRepository: ModesRepository,
    val connection: StateFlow<String>,
) : ViewModel() {

    val lights: StateFlow<List<Light>> = repository.lights

    /** Les 4 modes, dans l'ordre des touches du bouton MOES. */
    val modes: StateFlow<List<Mode>> = modesRepository.modes

    fun toggle(entityId: String) {
        viewModelScope.launch { repository.toggle(entityId) }
    }

    fun setAll(on: Boolean) {
        viewModelScope.launch { repository.setAll(on) }
    }

    fun applyMode(mode: Mode) {
        viewModelScope.launch { modesRepository.apply(mode) }
    }

    /**
     * Le switch unique : ON = jouer le mode (ses prises allumees, les autres
     * eteintes) ; OFF = tout eteindre.
     */
    fun setModeOn(mode: Mode, on: Boolean) {
        viewModelScope.launch {
            if (on) modesRepository.apply(mode) else repository.setAll(false)
        }
    }

    /** Redéfinit un mode 2-4 avec [states] (entityId → on/off). */
    fun saveMode(number: Int, states: Map<String, Boolean>) {
        viewModelScope.launch { modesRepository.save(number, states) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Portal6App
                LightsViewModel(app.container.lights, app.container.modes, app.container.connection)
            }
        }
    }
}
