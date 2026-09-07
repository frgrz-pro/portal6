package com.portal6.haremote.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal6.haremote.data.radio.Station

/**
 * Mini player AzuraCast : la liste des stations publiques du serveur, et pour
 * celle qui joue, pochette + artiste/titre + bouton Stop. Une seule station à la
 * fois ; appuyer sur une autre bascule le flux.
 */
@Composable
fun RadioScreen(
    viewModel: RadioViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val art by viewModel.art.collectAsStateWithLifecycle()
    val url by viewModel.azuracastUrl.collectAsStateWithLifecycle()

    // Android 13+ : sans cette permission la notification média n'apparaît pas
    // (la lecture, elle, marche quand même). Demandée au premier Play.
    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* le résultat n'a pas d'importance pour la lecture */ }
    val play: (Station) -> Unit = { station ->
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        viewModel.play(station)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Radio", style = MaterialTheme.typography.titleLarge)
        Text(
            url.removePrefix("http://").removePrefix("https://"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        NowPlayingCard(
            station = state.playing,
            art = art?.asImageBitmap(),
            isBuffering = state.isBuffering,
            onStop = viewModel::stop,
        )

        state.playerError?.let { StatusLine(it, isError = true) }
        state.error?.let { StatusLine(it, isError = true) }
        if (state.error == null && state.stations.isEmpty()) {
            StatusLine("Aucune station publique sur ce serveur (ou chargement en cours).", isError = false)
        }

        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.stations, key = { it.id }) { station ->
                StationRow(
                    station = station,
                    isPlaying = station.shortcode == state.playingShortcode,
                    onClick = {
                        if (station.shortcode == state.playingShortcode) viewModel.stop() else play(station)
                    },
                )
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, isError: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun NowPlayingCard(
    station: Station?,
    art: androidx.compose.ui.graphics.ImageBitmap?,
    isBuffering: Boolean,
    onStop: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (station != null) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Radio, contentDescription = null, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station?.name ?: "Rien ne joue",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        station == null -> "Choisis une station ci-dessous."
                        isBuffering -> "Connexion au flux…"
                        station.nowPlaying.isBlank() -> "En direct"
                        else -> station.nowPlaying
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station?.isLive == true) {
                    Text(
                        "LIVE · ${station.streamer}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (station != null) {
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.size(56.dp),
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(Icons.Filled.Stop, contentDescription = "Arrêter", modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    station: Station,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Arrêter" else "Écouter",
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(station.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    station.nowPlaying.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${station.listeners} 🎧",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
