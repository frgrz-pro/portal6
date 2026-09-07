package com.portal6.haremote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Interrupteur façon « rocker » : une piste sombre, un curseur qui occupe la
 * moitié de la piste — en bas (ou à gauche) et gris quand c'est éteint (cercle
 * creux), en haut (ou à droite) et ambre quand c'est allumé (icône power), la
 * piste se teintant d'ambre. Couleurs fixes, indépendantes du thème : c'est
 * un objet physique, pas une surface Material.
 */
@Composable
fun SocketSwitch(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** Couché : le curseur va de gauche (off) à droite (on). */
    horizontal: Boolean = false,
) {
    val track by animateColorAsState(if (checked) TrackOn else TrackOff, label = "track")
    val thumb by animateColorAsState(if (checked) ThumbOn else ThumbOff, label = "thumb")
    val bias by animateFloatAsState(
        targetValue = if (checked) -1f else 1f,
        animationSpec = spring(stiffness = 600f),
        label = "thumbPosition",
    )

    Box(
        modifier = modifier
            .aspectRatio(if (horizontal) 1 / 0.55f else 0.55f)
            .clip(RoundedCornerShape(percent = 32))
            .background(track)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(5.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(if (horizontal) BiasAlignment(-bias, 0f) else BiasAlignment(0f, bias))
                .then(
                    if (horizontal) Modifier.fillMaxHeight().fillMaxWidth(0.5f)
                    else Modifier.fillMaxWidth().fillMaxHeight(0.5f),
                )
                .clip(RoundedCornerShape(percent = 28))
                .background(thumb),
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Box(Modifier.size(12.dp).border(1.5.dp, Color.White, CircleShape))
            }
        }
    }
}

private val TrackOff = Color(0xFF363636)
private val ThumbOff = Color(0xFFA1A1A1)
private val TrackOn = Color(0xFF4A3B1C)
private val ThumbOn = Color(0xFFFFC233)
