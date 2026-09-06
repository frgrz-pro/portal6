package com.portal6.remote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portal6.remote.data.Light
import com.portal6.remote.data.LightsRepository
import com.portal6.remote.data.MockLightsRepository
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
