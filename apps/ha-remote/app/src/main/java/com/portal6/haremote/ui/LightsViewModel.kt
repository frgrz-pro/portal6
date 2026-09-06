package com.portal6.haremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.LightsRepository
import com.portal6.haremote.data.MockLightsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LightsViewModel(
    private val repository: LightsRepository = MockLightsRepository(),
) : ViewModel() {

    val lights: StateFlow<List<Light>> = repository.lights

    fun toggle(entityId: String) {
        viewModelScope.launch { repository.toggle(entityId) }
    }

    fun setAll(on: Boolean) {
        viewModelScope.launch { repository.setAll(on) }
    }
}
