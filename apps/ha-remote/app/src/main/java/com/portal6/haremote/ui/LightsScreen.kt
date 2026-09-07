package com.portal6.haremote.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal6.haremote.R
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.Mode
import com.portal6.haremote.qs.ModesTileService
import com.portal6.haremote.qs.TvMuteTileService

@Composable
fun LightsScreen(
    viewModel: LightsViewModel,
    modifier: Modifier = Modifier,
) {
    val lights by viewModel.lights.collectAsStateWithLifecycle()
    val modes by viewModel.modes.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val allOn = lights.isNotEmpty() && lights.all { it.isOn }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        ModesRow(
            modes = modes,
            lights = lights,
            onApply = viewModel::applyMode,
            onEdit = { editing = it.number },
        )

        Spacer(Modifier.height(12.dp))

        // Grille 2 colonnes × 4 : colonne gauche = multiprise A, droite = B.
        // L'ordre de DefaultLights est A1..A4 puis B1..B4 ; la grille remplit
        // ligne par ligne, donc on entrelace pour garder A à gauche.
        val interleaved = run {
            val half = lights.size / 2
            (0 until half).flatMap { i -> listOf(lights[i], lights[i + half]) }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(interleaved, key = { it.entityId }) { light ->
                LightButton(light = light, onClick = { viewModel.toggle(light.entityId) })
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("All", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(horizontal = 8.dp))
            Switch(checked = allOn, onCheckedChange = { viewModel.setAll(it) })
            Spacer(Modifier.weight(1f))
            Button(onClick = { viewModel.setAll(false) }) {
                Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Turn off")
            }
        }

        QuickSettingsTilesRow()

        Text(
            connection,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    editing?.let { number ->
        val mode = modes.firstOrNull { it.number == number } ?: return@let
        ModeDialog(
            mode = mode,
            lights = lights,
            onSave = { states ->
                viewModel.saveMode(number, states)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/**
 * Les 4 modes = les 4 touches d'un bouton MOES. Appui : jouer le mode.
 * Appui long sur un mode 2-4 : le redéfinir. Le mode dont l'état correspond
 * aux prises est mis en avant.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModesRow(
    modes: List<Mode>,
    lights: List<Light>,
    onApply: (Mode) -> Unit,
    onEdit: (Mode) -> Unit,
) {
    Column {
        Text("Modes", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            modes.forEach { mode ->
                val active = mode.matches(lights)
                val onColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            active -> MaterialTheme.colorScheme.primary
                            mode.isDefined -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { onApply(mode) },
                            onLongClick = { if (mode.isEditable) onEdit(mode) },
                        ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.titleSmall, color = onColor)
                        Text(
                            when {
                                mode.isAllToggle -> "tout on/off"
                                mode.isDefined -> "défini"
                                else -> "à définir"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = onColor,
                        )
                    }
                }
            }
        }
        Text(
            "Appui long sur un mode 2-4 pour le redéfinir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Redéfinir un mode : 8 interrupteurs, pré-remplis avec la définition du mode
 * (ou l'état courant des prises s'il n'en a pas encore). « État actuel »
 * recopie les prises telles qu'elles sont.
 */
@Composable
private fun ModeDialog(
    mode: Mode,
    lights: List<Light>,
    onSave: (Map<String, Boolean>) -> Unit,
    onDismiss: () -> Unit,
) {
    val initial = remember(mode.number) {
        lights.associate { it.entityId to (mode.states?.get(it.entityId) ?: it.isOn) }
    }
    var draft by remember(mode.number) { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Définir ${mode.label}") },
        text = {
            Column {
                Text(
                    "Ce que fait la touche ${mode.number} du bouton et le mode ${mode.number} de l'app.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                lights.forEach { light ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(light.label, modifier = Modifier.weight(1f))
                        Switch(
                            checked = draft[light.entityId] == true,
                            onCheckedChange = { on -> draft = draft + (light.entityId to on) },
                        )
                    }
                }
                TextButton(onClick = { draft = lights.associate { it.entityId to it.isOn } }) {
                    Text("Prendre l'état actuel")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/**
 * Ajout des tuiles au volet des réglages rapides sans passer par le mode
 * édition du système. L'API n'existe qu'à partir d'Android 13 ; en dessous,
 * l'ajout reste manuel (crayon « Modifier » du volet).
 */
@Composable
private fun QuickSettingsTilesRow() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Text(
            "Tuiles Modes et Mute TV : à ajouter via le crayon du volet des réglages rapides.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = {
            context.requestTile(ModesTileService::class.java, "Modes", R.drawable.ic_qs_salon)
        }) { Text("+ Tuile Modes") }
        TextButton(onClick = {
            context.requestTile(TvMuteTileService::class.java, "Mute TV", R.drawable.ic_qs_mute)
        }) { Text("+ Tuile Mute TV") }
    }
}

private fun Context.requestTile(service: Class<*>, label: String, iconRes: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    getSystemService(StatusBarManager::class.java)?.requestAddTileService(
        ComponentName(this, service),
        label,
        Icon.createWithResource(this, iconRes),
        mainExecutor,
    ) { /* résultat géré par le dialogue système */ }
}

@Composable
private fun LightButton(
    light: Light,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (light.isOn) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.aspectRatio(1.6f),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = if (light.isOn) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = if (light.isOn) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(light.label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
