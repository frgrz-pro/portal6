package com.portal6.haremote.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal6.haremote.data.Light

@Composable
fun LightsScreen(
    viewModel: LightsViewModel,
    modifier: Modifier = Modifier,
) {
    val lights by viewModel.lights.collectAsStateWithLifecycle()
    val allOn = lights.isNotEmpty() && lights.all { it.isOn }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

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
    }
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
