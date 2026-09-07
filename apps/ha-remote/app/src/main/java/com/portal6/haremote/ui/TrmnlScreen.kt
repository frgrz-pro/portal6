package com.portal6.haremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.portal6.haremote.Portal6App
import com.portal6.haremote.data.trmnl.TrmnlRepository
import com.portal6.haremote.data.trmnl.TrmnlScreen
import com.portal6.haremote.data.trmnl.TrmnlState
import kotlinx.coroutines.flow.StateFlow

class TrmnlViewModel(private val repo: TrmnlRepository) : ViewModel() {
    val state: StateFlow<TrmnlState> = repo.state
    fun reload() = repo.reload()
    fun setInRotation(screen: TrmnlScreen, on: Boolean) = repo.setInRotation(screen, on)
    fun only(screen: TrmnlScreen) = repo.only(screen)
    fun move(screen: TrmnlScreen, delta: Int) = repo.move(screen, delta)
    fun setRefreshInterval(seconds: Int) = repo.setRefreshInterval(seconds)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Portal6App
                TrmnlViewModel(app.container.trmnl)
            }
        }
    }
}

/** Choix de refresh proposés (secondes → libellé). 5 min exige TRMNL+, sinon l'API refuse. */
private val REFRESH_CHOICES = listOf(300 to "5 min", 900 to "15 min", 3600 to "1 h", 21600 to "6 h")

/**
 * La rotation des écrans du TRMNL : un switch par instance de plugin (dans la
 * rotation ou pas), l'ordre, le rythme de refresh du device. Le TRMNL tire son
 * image à chaque check-in : rien n'est instantané, l'UI le rappelle.
 */
@Composable
fun TrmnlScreen(
    viewModel: TrmnlViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val device = state.device
    val rotation = state.screens.filter { it.item != null }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TRMNL", style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        state.demo -> "Mode démo — clé TRMNL non renseignée (onglet Réglages)"
                        device == null -> "Chargement…"
                        else -> "${device.name} · batterie ${device.batteryPercent} % · check-in toutes les ${device.refreshInterval.label()}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::reload, enabled = !state.loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Recharger")
            }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        if (device != null) {
            Spacer(Modifier.height(12.dp))
            Text("Refresh du device", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                REFRESH_CHOICES.forEach { (seconds, label) ->
                    FilterChip(
                        selected = device.refreshInterval == seconds,
                        onClick = { if (device.refreshInterval != seconds) viewModel.setRefreshInterval(seconds) },
                        label = { Text(label) },
                        enabled = !state.loading,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Un changement n'est visible qu'au prochain check-in du TRMNL.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.screens, key = { it.plugin.id }) { screen ->
                val index = rotation.indexOf(screen)
                ScreenRow(
                    screen = screen,
                    enabled = !state.loading,
                    canMoveUp = index > 0,
                    canMoveDown = index >= 0 && index < rotation.lastIndex,
                    onToggle = { viewModel.setInRotation(screen, it) },
                    onOnly = { viewModel.only(screen) },
                    onMove = { viewModel.move(screen, it) },
                )
            }
        }
    }
}

private fun Int.label(): String = REFRESH_CHOICES.firstOrNull { it.first == this }?.second ?: "$this s"

@Composable
private fun ScreenRow(
    screen: TrmnlScreen,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onOnly: () -> Unit,
    onMove: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (screen.inRotation) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(screen.plugin.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        screen.item == null -> "Pas dans la playlist"
                        screen.item.visible -> "En rotation" + (screen.plugin.strategy?.let { " · $it" } ?: "")
                        else -> "En pause"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (enabled) {
                    TextButton(onClick = onOnly, contentPadding = PaddingValues(0.dp)) {
                        Text("Uniquement celui-ci", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            if (screen.item != null) {
                Column {
                    IconButton(onClick = { onMove(-1) }, enabled = enabled && canMoveUp) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Monter")
                    }
                    IconButton(onClick = { onMove(1) }, enabled = enabled && canMoveDown) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Descendre")
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Switch(checked = screen.inRotation, onCheckedChange = onToggle, enabled = enabled)
        }
    }
}
