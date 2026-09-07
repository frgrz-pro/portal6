package com.portal6.haremote.qs

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.portal6.haremote.container
import com.portal6.haremote.data.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Tuile « Modes » du volet des réglages rapides : un appui passe au mode
 * suivant parmi les modes 2-4 définis (2 → 3 → 4 → 2). Le sous-titre indique
 * le mode qui correspond à l'état courant des prises, s'il y en a un.
 * Le mode 1 (tout on/off) a sa place dans l'app, pas dans un cycle.
 */
class ModesTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val app = container
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { s ->
            s.launch {
                combine(app.modes.modes, app.lights.lights) { _, _ -> Unit }.collect { render() }
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val app = container
        val candidates = app.modes.modes.value.filter { it.isEditable && it.isDefined }
        if (candidates.isEmpty()) return
        val lights = app.lights.lights.value
        val current = candidates.indexOfFirst { it.matches(lights) }
        val next = candidates[(current + 1) % candidates.size]
        app.appScope.launch {
            app.modes.apply(next)
            render()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val app = container
        val modes = app.modes.modes.value
        val defined = modes.filter { it.isEditable && it.isDefined }
        val active: Mode? = modes.firstOrNull { it.matches(app.lights.lights.value) }

        tile.label = "Modes"
        tile.state = when {
            defined.isEmpty() -> Tile.STATE_UNAVAILABLE
            active != null -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                defined.isEmpty() -> "Aucun mode défini"
                active != null -> active.label
                else -> "Appuyer : mode suivant"
            }
        }
        tile.updateTile()
    }
}
