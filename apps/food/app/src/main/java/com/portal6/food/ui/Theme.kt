package com.portal6.food.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Mêmes tokens que le portail : fond sombre/clair auto, accent bleu, jauge jaune. */
val Yellow = Color(0xFFF5C518)
val YellowDark = Color(0xFFFBBF24)
val Ok = Color(0xFF15803D)
val OkDark = Color(0xFF4ADE80)
val Bad = Color(0xFFB91C1C)
val BadDark = Color(0xFFF87171)

private val Dark = darkColorScheme(
    primary = Color(0xFF6EA8FE),
    onPrimary = Color(0xFF0F1115),
    background = Color(0xFF0F1115),
    surface = Color(0xFF171A21),
    surfaceVariant = Color(0xFF1E222B),
    onSurfaceVariant = Color(0xFF98A0AE),
    outline = Color(0xFF2A2F3A),
    onBackground = Color(0xFFE6E8EC),
    onSurface = Color(0xFFE6E8EC),
)

private val Light = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    background = Color(0xFFF6F7F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF5C6675),
    outline = Color(0xFFDFE3EA),
    onBackground = Color(0xFF1A1D23),
    onSurface = Color(0xFF1A1D23),
)

@Composable
fun FoodTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

@Composable fun yellow() = if (isSystemInDarkTheme()) YellowDark else Yellow
@Composable fun ok() = if (isSystemInDarkTheme()) OkDark else Ok
@Composable fun bad() = if (isSystemInDarkTheme()) BadDark else Bad
