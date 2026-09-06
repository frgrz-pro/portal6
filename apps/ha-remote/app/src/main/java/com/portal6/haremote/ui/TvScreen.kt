package com.portal6.haremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Le bouton MUTE et la tuile des réglages rapides partagent le même état
 * ([TvRepository]). Ce qui manque encore : l'envoi réel de
 * KEYCODE_VOLUME_MUTE à la Shield via le protocole Android TV Remote v2
 * (phase 2, cf. .docs/tv-mute.md).
 */
@Composable
fun TvScreen(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier,
) {
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        FilledIconButton(
            onClick = viewModel::toggleMute,
            colors = if (isMuted) {
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                IconButtonDefaults.filledIconButtonColors()
            },
            modifier = Modifier.size(160.dp),
        ) {
            Icon(
                imageVector = if (isMuted) {
                    Icons.AutoMirrored.Filled.VolumeOff
                } else {
                    Icons.AutoMirrored.Filled.VolumeUp
                },
                contentDescription = if (isMuted) "Rétablir le son" else "Couper le son",
                modifier = Modifier.size(80.dp),
            )
        }
        Text(
            if (isMuted) "SON COUPÉ" else "SON ACTIF",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Envoi réel à la Shield : phase 2",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
