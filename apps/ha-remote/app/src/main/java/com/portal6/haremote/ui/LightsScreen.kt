package com.portal6.haremote.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal6.haremote.R
import com.portal6.haremote.data.Light
import com.portal6.haremote.data.Mode
import com.portal6.haremote.qs.ModesTileService
import com.portal6.haremote.qs.TvMuteTileService

/**
 * Onglet Lights, en trois zones :
 * 1. la rangée des **4 modes** — appui sur un mode 2-4 = le (re)définir,
 *    appui sur le mode 1 = le sélectionner (toutes les prises, pas éditable) ;
 * 2. la grille des **8 tuiles lampes** — hors édition elles montrent l'état
 *    réel des prises (et un appui bascule la prise) ; en édition elles servent
 *    de **filtre** : on coche celles qui font partie du mode ;
 * 3. le **switch unique** : ON = jouer le mode sélectionné (ses prises
 *    allumées, les autres éteintes), OFF = tout éteindre.
 */
@Composable
fun LightsScreen(
    viewModel: LightsViewModel,
    modifier: Modifier = Modifier,
) {
    val lights by viewModel.lights.collectAsStateWithLifecycle()
    val modes by viewModel.modes.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    // Mode sélectionné : celui choisi par l'utilisateur, sinon celui qui
    // correspond à l'état des prises, sinon le mode 1.
    var chosen by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected: Mode? = modes.firstOrNull { it.number == chosen }
        ?: modes.firstOrNull { it.matches(lights) }
        ?: modes.firstOrNull()

    // Édition : numéro du mode en cours de définition + filtre en brouillon.
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    var draft by rememberSaveable { mutableStateOf(setOf<String>()) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        ModesRow(
            modes = modes,
            selected = selected?.number,
            editing = editing,
            onSelect = { mode ->
                if (mode.isEditable) {
                    editing = mode.number
                    draft = mode.states?.filterValues { it }?.keys ?: emptySet()
                } else {
                    editing = null
                    chosen = mode.number
                }
            },
        )

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(lights, key = { it.entityId }) { light ->
                if (editing != null) {
                    LampTile(
                        label = light.label,
                        lit = light.entityId in draft,
                        selecting = true,
                        onClick = {
                            draft = if (light.entityId in draft) draft - light.entityId else draft + light.entityId
                        },
                    )
                } else {
                    LampTile(
                        label = light.label,
                        lit = light.isOn,
                        selecting = false,
                        onClick = { viewModel.toggle(light.entityId) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val editingMode = modes.firstOrNull { it.number == editing }
        if (editingMode != null) {
            EditBar(
                mode = editingMode,
                count = draft.size,
                onCancel = { editing = null },
                onSave = {
                    viewModel.saveMode(editingMode.number, lights.associate { it.entityId to (it.entityId in draft) })
                    chosen = editingMode.number
                    editing = null
                },
            )
        } else if (selected != null) {
            ModeSwitchRow(
                mode = selected,
                lights = lights,
                onToggle = { on -> viewModel.setModeOn(selected, on) },
            )
        }

        QuickSettingsTilesRow()

        Text(
            connection,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Les 4 modes = les 4 touches d'un bouton MOES. */
@Composable
private fun ModesRow(
    modes: List<Mode>,
    selected: Int?,
    editing: Int?,
    onSelect: (Mode) -> Unit,
) {
    Column {
        Text("Modes", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            modes.forEach { mode ->
                val highlighted = if (editing != null) mode.number == editing else mode.number == selected
                val onColor = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                Card(
                    onClick = { onSelect(mode) },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            highlighted -> MaterialTheme.colorScheme.primary
                            mode.isDefined -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.titleSmall, color = onColor)
                        Text(
                            when {
                                mode.number == editing -> "édition"
                                mode.isAllToggle -> "toutes"
                                mode.isDefined -> "${mode.states!!.count { it.value }} prises"
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
            if (editing != null) "Coche les prises du mode, puis Enregistrer."
            else "Appui sur un mode 2-4 pour choisir ses prises.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Une tuile lampe. Hors édition : ambre = prise allumée, gris = éteinte.
 * En édition ([selecting]) : ambre + coche = fait partie du mode.
 */
@Composable
private fun LampTile(
    label: String,
    lit: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
) {
    val container by animateColorAsState(
        if (lit) LampOn else MaterialTheme.colorScheme.surfaceVariant,
        label = "tile",
    )
    val content = if (lit) LampOnContent else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.aspectRatio(0.85f),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = if (lit) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.titleMedium, color = content)
            }
            if (selecting && lit) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "dans le mode",
                    tint = content,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                )
            }
        }
    }
}

/** Barre d'édition : annuler, ou enregistrer le filtre (au moins une prise). */
@Composable
private fun EditBar(
    mode: Mode,
    count: Int,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "${mode.label} : $count prise${if (count > 1) "s" else ""}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = onCancel) { Text("Annuler") }
        Button(onClick = onSave, enabled = count > 0) { Text("Enregistrer") }
    }
}

/** Le switch unique : le mode sélectionné, ses prises, et le rocker ON/OFF. */
@Composable
private fun ModeSwitchRow(
    mode: Mode,
    lights: List<Light>,
    onToggle: (Boolean) -> Unit,
) {
    val on = mode.matches(lights)
    val members = if (mode.isAllToggle) "toutes les prises"
        else mode.states?.filterValues { it }?.keys
            ?.let { ids -> lights.filter { it.entityId in ids }.joinToString(", ") { it.label } }
            ?.ifEmpty { null }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(mode.label, style = MaterialTheme.typography.titleLarge)
            Text(
                members ?: "à définir — appuie sur le mode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (mode.isDefined) {
            SocketSwitch(checked = on, onToggle = { onToggle(!on) }, modifier = Modifier.height(96.dp))
        }
    }
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

/** Ambre des lampes allumées — le même que le curseur du SocketSwitch. */
private val LampOn = Color(0xFFFFC233)
private val LampOnContent = Color(0xFF3A2A00)
