package com.portal6.haremote.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Frontière côté TV, calquée sur [LightsRepository]. L'implémentation réelle
 * enverra `KEYCODE_VOLUME_MUTE` à la Shield via le protocole Android TV
 * Remote v2 (phase 2, cf. .docs/tv-mute.md).
 *
 * En attendant, [MockTvRepository] ne tient qu'un booléen : la tuile et l'écran
 * TV sont donc déjà câblés et testables, seul l'envoi réel manque.
 */
interface TvRepository {
    val isMuted: StateFlow<Boolean>
    suspend fun toggleMute()
}

class MockTvRepository(private val store: ConfigStore) : TvRepository {

    private val _isMuted = MutableStateFlow(store.loadMuted())
    override val isMuted: StateFlow<Boolean> = _isMuted

    override suspend fun toggleMute() {
        _isMuted.value = !_isMuted.value
        store.saveMuted(_isMuted.value)
    }
}
