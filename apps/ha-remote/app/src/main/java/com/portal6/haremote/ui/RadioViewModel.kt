package com.portal6.haremote.ui

import android.app.Application
import android.content.ComponentName
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.portal6.haremote.Portal6App
import com.portal6.haremote.data.SettingsStore
import com.portal6.haremote.data.radio.AzuraClient
import com.portal6.haremote.data.radio.Station
import com.portal6.haremote.player.RadioService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Ce que l'écran Radio affiche : la liste des stations et l'état du lecteur. */
data class RadioUiState(
    val stations: List<Station> = emptyList(),
    val error: String? = null,
    /** Station en cours de lecture (ou en train de démarrer), identifiée par son `shortcode`. */
    val playingShortcode: String? = null,
    val isBuffering: Boolean = false,
    val playerError: String? = null,
) {
    val playing: Station? get() = stations.firstOrNull { it.shortcode == playingShortcode }
}

/**
 * Sonde `/api/nowplaying` toutes les [POLL_MS] tant que l'écran est affiché,
 * et pilote le [RadioService] via un `MediaController` — le même chemin que la
 * notification, donc un seul état de vérité : le lecteur.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RadioViewModel(
    private val app: Application,
    private val settings: SettingsStore,
) : ViewModel() {

    val azuracastUrl: StateFlow<String> = settings.azuracastUrl

    private val _playing = MutableStateFlow<String?>(null)
    private val _buffering = MutableStateFlow(false)
    private val _playerError = MutableStateFlow<String?>(null)

    private val polled: StateFlow<Pair<List<Station>, String?>> = settings.azuracastUrl
        .flatMapLatest { url ->
            flow {
                val client = AzuraClient(url)
                var last: List<Station> = emptyList()
                while (true) {
                    runCatching { client.nowPlaying() }
                        .onSuccess { last = it; emit(it to null) }
                        .onFailure { emit(last to "AzuraCast injoignable ($url) : ${it.message}") }
                    delay(POLL_MS)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Station>() to null)

    val state: StateFlow<RadioUiState> =
        combine(polled, _playing, _buffering, _playerError) { (stations, error), playing, buffering, playerError ->
            RadioUiState(stations, error, playing, buffering, playerError)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RadioUiState())

    /** Pochette de la station en cours, rechargée quand l'URL de l'image change. */
    val art: StateFlow<Bitmap?> = state
        .map { it.playing?.artUrl }
        .distinctUntilChanged()
        .flatMapLatest { url ->
            flow { emit(if (url == null) null else AzuraClient(azuracastUrl.value).art(url)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)

        override fun onPlayerError(error: PlaybackException) {
            _playerError.value = "Lecture impossible : ${error.errorCodeName}"
            _playing.value = null
            _buffering.value = false
        }
    }

    /** Recopie l'état du lecteur (source de vérité) dans les flows de l'UI. */
    private fun sync(player: Player) {
        val active = player.playWhenReady && player.playbackState != Player.STATE_IDLE
        _playing.value = if (active) player.currentMediaItem?.mediaId else null
        _buffering.value = active && player.playbackState == Player.STATE_BUFFERING
        if (active) _playerError.value = null
    }

    init {
        val token = SessionToken(app, ComponentName(app, RadioService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync().also { future ->
            future.addListener({
                val c = runCatching { future.get() }.getOrNull() ?: return@addListener
                controller = c
                c.addListener(listener)
                sync(c)
            }, ContextCompat.getMainExecutor(app))
        }
    }

    fun play(station: Station) {
        val c = controller ?: return
        _playerError.value = null
        _playing.value = station.shortcode
        val item = MediaItem.Builder()
            .setMediaId(station.shortcode)
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("AzuraCast")
                    .setArtworkUri(station.artUrl?.let(Uri::parse))
                    .build(),
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    /** Un flux live ne se met pas en pause : on arrête et on libère le flux. */
    fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
        }
        _playing.value = null
        _buffering.value = false
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    companion object {
        const val POLL_MS = 15_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Portal6App
                RadioViewModel(app, app.container.settings)
            }
        }
    }
}
