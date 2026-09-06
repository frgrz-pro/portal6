package com.portal6.haremote.qs

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.portal6.haremote.container
import com.portal6.haremote.data.Rooms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Tuile « Salon » du volet des réglages rapides : un appui passe à la config
 * suivante de la pièce, en boucle. Le sous-titre affiche la config active.
 *
 * Les configs se créent depuis l'app (onglet Lights) — tant qu'il n'y en a
 * aucune, la tuile est grisée.
 */
class SalonConfigTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val store = container.store
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { s ->
            s.launch {
                combine(store.configs, store.activeConfigId) { _, _ -> Unit }
                    .collect { render() }
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
        val next = app.store.nextConfig(Rooms.SALON_ID) ?: return
        app.appScope.launch {
            app.lights.apply(next.states)
            app.store.setActiveConfig(next.id)
            render()
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val store = container.store
        val configs = store.configsOf(Rooms.SALON_ID)
        val active = configs.firstOrNull { it.id == store.activeConfigId.value }

        tile.label = Rooms.Salon.label
        tile.state = when {
            configs.isEmpty() -> Tile.STATE_UNAVAILABLE
            active != null -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                configs.isEmpty() -> "Aucune config"
                active != null -> active.name
                else -> "Appuyer pour appliquer"
            }
        }
        tile.updateTile()
    }
}
