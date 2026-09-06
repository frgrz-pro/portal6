package com.portal6.haremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Phase 2 (cf. .docs/tv-mute.md) : le gros bouton MUTE enverra
 * KEYCODE_VOLUME_MUTE à la Shield via le protocole Android TV Remote v2.
 * Pour l'instant : maquette inerte, le temps de valider la chaîne CEC.
 */
@Composable
fun TvScreen(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        FilledIconButton(
            onClick = { /* phase 2 : mute Shield via Remote v2 */ },
            modifier = Modifier.size(160.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = "Mute",
                modifier = Modifier.size(80.dp),
            )
        }
        Text(
            "MUTE — bientôt",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
