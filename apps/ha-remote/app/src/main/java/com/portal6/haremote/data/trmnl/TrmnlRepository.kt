package com.portal6.haremote.data.trmnl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Une ligne de l'onglet TRMNL : une instance de plugin et sa place dans la rotation. */
data class TrmnlScreen(
    val plugin: TrmnlPlugin,
    /** `null` si l'instance n'est dans aucune playlist du device. */
    val item: TrmnlPlaylistItem?,
) {
    val inRotation: Boolean get() = item?.visible == true
}

data class TrmnlState(
    val device: TrmnlDevice? = null,
    /** Écrans en rotation d'abord (dans l'ordre de la playlist), puis les autres. */
    val screens: List<TrmnlScreen> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    /** Vrai quand l'app est sans clé : données simulées. */
    val demo: Boolean = false,
)

/**
 * La rotation des écrans du TRMNL, vue de l'app : quels plugins tournent, dans
 * quel ordre, à quel rythme. Chaque action relit tout derrière : l'API ne
 * pousse rien, et `row_order` est recalculé côté TRMNL.
 */
interface TrmnlRepository {
    val state: StateFlow<TrmnlState>
    fun reload()
    fun setInRotation(screen: TrmnlScreen, on: Boolean)
    /** Ne garde que cet écran visible — le « afficher X » du pauvre, effectif au prochain check-in. */
    fun only(screen: TrmnlScreen)
    fun move(screen: TrmnlScreen, delta: Int)
    fun setRefreshInterval(seconds: Int)
}

private fun List<TrmnlScreen>.rotationFirst() =
    sortedWith(compareBy({ it.item == null }, { it.item?.rowOrder ?: Int.MAX_VALUE }, { it.plugin.name }))

/** Client réel : un seul device (le premier du compte) en v1, l'id est gardé pour la suite. */
class HttpTrmnlRepository(
    private val client: TrmnlClient,
    private val scope: CoroutineScope,
) : TrmnlRepository {

    private val _state = MutableStateFlow(TrmnlState(loading = true))
    override val state: StateFlow<TrmnlState> = _state

    init { reload() }

    override fun reload() = run { load() }

    private fun run(block: suspend () -> Unit) {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { block() }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Erreur TRMNL") }
        }
    }

    private suspend fun load() {
        val device = client.devices().firstOrNull()
            ?: throw TrmnlClient.TrmnlException("Aucun device sur ce compte TRMNL")
        val plugins = client.pluginSettings()
        val items = client.playlist(device.id).associateBy { it.pluginSettingId }
        _state.value = TrmnlState(
            device = device,
            screens = plugins.map { TrmnlScreen(it, items[it.id]) }.rotationFirst(),
        )
    }

    private fun deviceId() = _state.value.device?.id ?: throw TrmnlClient.TrmnlException("Device inconnu")

    override fun setInRotation(screen: TrmnlScreen, on: Boolean) = run {
        val item = screen.item
        when {
            item == null && on -> client.addToPlaylist(deviceId(), screen.plugin.id)
            item != null && item.visible != on -> client.setVisible(item.id, on)
        }
        load()
    }

    override fun only(screen: TrmnlScreen) = run {
        val id = deviceId()
        val target = screen.item ?: client.addToPlaylist(id, screen.plugin.id)
        client.playlist(id).forEach { item ->
            val wanted = item.id == target.id
            if (item.visible != wanted) client.setVisible(item.id, wanted)
        }
        load()
    }

    override fun move(screen: TrmnlScreen, delta: Int) = run {
        val id = deviceId()
        val ids = client.playlist(id).map { it.id }.toMutableList()
        val from = ids.indexOf(screen.item?.id)
        val to = from + delta
        if (from >= 0 && to in ids.indices) {
            ids.add(to, ids.removeAt(from))
            client.reorder(id, ids)
        }
        load()
    }

    override fun setRefreshInterval(seconds: Int) = run {
        client.setRefreshInterval(deviceId(), seconds)
        load()
    }
}

/**
 * Suit la clé saisie dans Réglages : client réel quand elle est là, mock sinon.
 * Même principe que `BackendHolder` pour HA, mais la clé TRMNL est indépendante.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DelegatingTrmnlRepository(
    apiKey: StateFlow<String>,
    private val scope: CoroutineScope,
) : TrmnlRepository {

    private val current: StateFlow<TrmnlRepository> = apiKey
        .map { key -> if (key.isBlank()) MockTrmnlRepository() else HttpTrmnlRepository(TrmnlClient(key), scope) }
        .stateIn(scope, SharingStarted.Eagerly, MockTrmnlRepository())

    override val state: StateFlow<TrmnlState> = current
        .flatMapLatest { it.state }
        .stateIn(scope, SharingStarted.Eagerly, current.value.state.value)

    override fun reload() = current.value.reload()
    override fun setInRotation(screen: TrmnlScreen, on: Boolean) = current.value.setInRotation(screen, on)
    override fun only(screen: TrmnlScreen) = current.value.only(screen)
    override fun move(screen: TrmnlScreen, delta: Int) = current.value.move(screen, delta)
    override fun setRefreshInterval(seconds: Int) = current.value.setRefreshInterval(seconds)
}

/** Sans clé : une playlist en mémoire pour voir l'écran fonctionner. */
class MockTrmnlRepository : TrmnlRepository {

    private var items = listOf(
        TrmnlPlaylistItem(1, 100, visible = true, rowOrder = 1, renderedAt = null),
        TrmnlPlaylistItem(2, 101, visible = false, rowOrder = 2, renderedAt = null),
    )
    private val plugins = listOf(
        TrmnlPlugin(100, "portal6 — Dashboard", "polling"),
        TrmnlPlugin(101, "Agenda semaine", null),
        TrmnlPlugin(102, "Météo", null),
    )
    private var refresh = 900

    private val _state = MutableStateFlow(build())
    override val state: StateFlow<TrmnlState> = _state

    private fun build(): TrmnlState {
        val byPlugin = items.associateBy { it.pluginSettingId }
        return TrmnlState(
            device = TrmnlDevice(0, "TRMNL (démo)", "DEMO", 87, refresh, null),
            screens = plugins.map { TrmnlScreen(it, byPlugin[it.id]) }.rotationFirst(),
            demo = true,
        )
    }

    private fun publish() { _state.value = build() }

    override fun reload() = publish()

    override fun setInRotation(screen: TrmnlScreen, on: Boolean) {
        val item = screen.item
        items = when {
            item == null && on ->
                items + TrmnlPlaylistItem(items.maxOf { it.id } + 1, screen.plugin.id, true, items.size + 1, null)
            item == null -> items
            else -> items.map { if (it.id == item.id) it.copy(visible = on) else it }
        }
        publish()
    }

    override fun only(screen: TrmnlScreen) {
        if (screen.item == null) setInRotation(screen, true)
        items = items.map { it.copy(visible = it.pluginSettingId == screen.plugin.id) }
        publish()
    }

    override fun move(screen: TrmnlScreen, delta: Int) {
        val sorted = items.sortedBy { it.rowOrder }.toMutableList()
        val from = sorted.indexOfFirst { it.id == screen.item?.id }
        val to = from + delta
        if (from >= 0 && to in sorted.indices) sorted.add(to, sorted.removeAt(from))
        items = sorted.mapIndexed { i, it -> it.copy(rowOrder = i + 1) }
        publish()
    }

    override fun setRefreshInterval(seconds: Int) {
        refresh = seconds
        publish()
    }
}
