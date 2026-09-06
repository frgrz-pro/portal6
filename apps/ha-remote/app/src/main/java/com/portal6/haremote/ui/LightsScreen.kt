package com.portal6.haremote.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal6.haremote.R
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.RoomConfig
import com.portal6.haremote.data.Rooms
import com.portal6.haremote.qs.SalonConfigTileService
import com.portal6.haremote.qs.TvMuteTileService

@Composable
fun LightsScreen(
    viewModel: LightsViewModel,
    modifier: Modifier = Modifier,
) {
    val lights by viewModel.lights.collectAsStateWithLifecycle()
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val activeConfigId by viewModel.activeConfigId.collectAsStateWithLifecycle()
    val allOn = lights.isNotEmpty() && lights.all { it.isOn }
    var showConfigDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        ConfigsRow(
            configs = configs,
            activeConfigId = activeConfigId,
            onApply = viewModel::applyConfig,
            onManage = { showConfigDialog = true },
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
    }

    if (showConfigDialog) {
        ConfigDialog(
            configs = configs,
            onSave = viewModel::saveConfig,
            onDelete = viewModel::deleteConfig,
            onDismiss = { showConfigDialog = false },
        )
    }
}

/**
 * Les configs enregistrées du salon. Un appui rejoue la config ; le bouton
 * « + » ouvre la gestion (enregistrer l'état courant, supprimer).
 */
@Composable
private fun ConfigsRow(
    configs: List<RoomConfig>,
    activeConfigId: String?,
    onApply: (RoomConfig) -> Unit,
    onManage: () -> Unit,
) {
    Column {
        Text(Rooms.Salon.label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(configs, key = { it.id }) { config ->
                FilterChip(
                    selected = config.id == activeConfigId,
                    onClick = { onApply(config) },
                    label = { Text(config.name) },
                )
            }
            item {
                AssistChip(
                    onClick = onManage,
                    label = { Text(if (configs.isEmpty()) "Enregistrer la config" else "Gérer") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun ConfigDialog(
    configs: List<RoomConfig>,
    onSave: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configs du salon") },
        text = {
            Column {
                Text(
                    "Enregistre l'état actuel des 8 prises sous un nom. " +
                        "La tuile Salon des réglages rapides fait défiler ces configs.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom (ex. Soirée, Lecture)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                configs.forEach { config ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(config.name, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(config.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer " + config.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(name)
                    onDismiss()
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
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
            "Tuiles Salon et Mute TV : à ajouter via le crayon du volet des réglages rapides.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = {
            context.requestTile(SalonConfigTileService::class.java, "Salon", R.drawable.ic_qs_salon)
        }) { Text("+ Tuile Salon") }
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
