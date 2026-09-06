package com.portal6.haremote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portal6.haremote.Portal6App
import com.portal6.haremote.data.TvRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TvViewModel(private val repository: TvRepository) : ViewModel() {

    val isMuted: StateFlow<Boolean> = repository.isMuted

    fun toggleMute() {
        viewModelScope.launch { repository.toggleMute() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Portal6App
                TvViewModel(app.container.tv)
            }
        }
    }
}
