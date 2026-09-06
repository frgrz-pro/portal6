package com.portal6.haremote.qs

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.portal6.haremote.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Tuile « Mute TV » du volet des réglages rapides. Le câblage complet est là
 * (état partagé avec l'onglet TV, persisté) ; ce qui manque est l'envoi réel de
 * `KEYCODE_VOLUME_MUTE` à la Shield — phase 2, cf. .docs/tv-mute.md.
 */
class TvMuteTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { s ->
            s.launch { container.tv.isMuted.collect { render(it) } }
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
        app.appScope.launch { app.tv.toggleMute() }
    }

    private fun render(muted: Boolean) {
        val tile = qsTile ?: return
        tile.label = "Mute TV"
        tile.state = if (muted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (muted) "Coupé" else "Son actif"
        }
        tile.updateTile()
    }
}
