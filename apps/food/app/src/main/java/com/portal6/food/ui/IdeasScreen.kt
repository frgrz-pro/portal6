package com.portal6.food.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.portal6.food.domain.Coverage
import com.portal6.food.domain.DeckCard
import com.portal6.food.domain.FoodState
import com.portal6.food.domain.Moment
import com.portal6.food.domain.buildDeck
import com.portal6.food.domain.formatQty
import com.portal6.food.domain.plural
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun IdeasScreen(vm: FoodViewModel, state: FoodState, persons: Int) {
    val choice by vm.moment.collectAsStateWithLifecycle()
    val skipped by vm.skippedLeftovers.collectAsStateWithLifecycle()
    val auto = Moment.auto(LocalTime.now().hour)
    val moment: Moment? = when (val c = choice) {
        MomentChoice.Auto -> auto
        MomentChoice.All -> null
        is MomentChoice.Forced -> c.moment
    }
    val deck = remember(state, moment, persons, skipped) {
        state.buildDeck(moment, persons, skipped, System.currentTimeMillis())
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        // Chips de moment : auto (heure), un moment forcé, ou tout.
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = choice == MomentChoice.Auto, onClick = { vm.setMoment(MomentChoice.Auto) },
                label = { Text("Auto · ${auto?.label ?: "tout"}") })
            Moment.entries.forEach { m ->
                FilterChip(selected = choice == MomentChoice.Forced(m), onClick = { vm.setMoment(MomentChoice.Forced(m)) },
                    label = { Text(m.label) })
            }
            FilterChip(selected = choice == MomentChoice.All, onClick = { vm.setMoment(MomentChoice.All) }, label = { Text("Tout") })
        }
        Spacer(Modifier.height(10.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (deck.isEmpty()) {
                EmptyDeck(moment)
            } else {
                deck.take(3).reversed().forEachIndexed { i, card ->
                    val pos = minOf(deck.size, 3) - 1 - i // 0 = dessus
                    if (pos == 0) {
                        key(card.key) {
                            SwipeableCard(card, persons, onDecide = { like -> decide(vm, card, like) },
                                onTap = { card.recipe?.let { vm.openDetail(it.id, card.leftover?.id) } })
                        }
                    } else {
                        Box(Modifier.fillMaxSize().scale(1f - 0.04f * pos).offset(y = (10 * pos).dp).alpha(0.7f / pos)) {
                            RecipeCard(card, persons)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        val top = deck.firstOrNull()
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Filled.Close, "Pas maintenant", bad(), enabled = top != null) { top?.let { decide(vm, it, false) } }
            RoundButton(Icons.Filled.Info, "Détail", MaterialTheme.colorScheme.primary, size = 50.dp, enabled = top?.recipe != null) {
                top?.recipe?.let { vm.openDetail(it.id, top.leftover?.id) }
            }
            RoundButton(Icons.Filled.Favorite, "Je la garde", ok(), enabled = top != null) { top?.let { decide(vm, it, true) } }
        }
    }
}

private fun decide(vm: FoodViewModel, card: DeckCard, like: Boolean) {
    val l = card.leftover
    if (l != null) { if (like) vm.eatLeftover(l.id) else vm.skipLeftover(l.id) }
    else card.recipe?.let { vm.swipe(it.id, like) }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, size: androidx.compose.ui.unit.Dp = 62.dp, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(size),
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = tint)) {
        Icon(icon, label, Modifier.size(size / 2))
    }
}

@Composable
private fun EmptyDeck(moment: Moment?) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Plus rien à proposer", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            if (moment == null) "Toutes les recettes sont validées ou passées." else "Rien de plus pour « ${moment.label} » — essaie un autre moment.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
    }
}

/** La carte du dessus : glisser à droite = oui, à gauche = non, toucher = détail. */
@Composable
private fun SwipeableCard(card: DeckCard, persons: Int, onDecide: (Boolean) -> Unit, onTap: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Position mise à jour de façon synchrone pendant le geste (un swipe rapide n'émet que
    // quelques événements : pas de coroutine par événement), animée seulement au relâcher.
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var released by remember { mutableStateOf(false) }
    val threshold = 260f
    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(dx.roundToInt(), (dy * 0.3f).roundToInt()) }
            .graphicsLayer { rotationZ = dx / 40f }
            .pointerInput(card.key) {
                // Un seul détecteur pour tap + glisser : deux détecteurs séparés se marchent
                // dessus sur un swipe rapide (le tap se déclenche et ouvre la fiche).
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var dragging = false
                    var totalX = 0f
                    var totalY = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUp()) { change.consume(); break }
                        val delta = change.positionChange()
                        totalX += delta.x; totalY += delta.y
                        if (!dragging && (abs(totalX) > slop || abs(totalY) > slop)) dragging = true
                        if (dragging && !released) { change.consume(); dx += delta.x; dy += delta.y }
                    }
                    if (!dragging) { onTap(); return@awaitEachGesture }
                    if (released) return@awaitEachGesture
                    val decided = abs(dx) > threshold
                    val like = dx > 0
                    released = decided
                    scope.launch {
                        val from = dx
                        val to = if (decided) (if (like) 1600f else -1600f) else 0f
                        launch { animate(dy, 0f, animationSpec = tween(220)) { v, _ -> dy = v } }
                        animate(from, to, animationSpec = tween(if (decided) 260 else 220)) { v, _ -> dx = v }
                        if (decided) onDecide(like)
                    }
                }
            },
    ) {
        RecipeCard(card, persons)
        val yesAlpha = (dx / threshold).coerceIn(0f, 1f)
        val noAlpha = (-dx / threshold).coerceIn(0f, 1f)
        Badge(if (card.leftover != null) "MIAM" else "OUI", ok(), yesAlpha, Modifier.align(Alignment.TopStart).padding(22.dp).graphicsLayer { rotationZ = -14f })
        Badge(if (card.leftover != null) "PLUS TARD" else "NOPE", bad(), noAlpha, Modifier.align(Alignment.TopEnd).padding(22.dp).graphicsLayer { rotationZ = 14f })
    }
}

@Composable
private fun Badge(text: String, color: Color, alpha: Float, modifier: Modifier) {
    if (alpha <= 0f) return
    Text(text, modifier = modifier.alpha(alpha).border(3.dp, color, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 6.dp),
        color = color, fontWeight = FontWeight.Black, fontSize = 26.sp)
}

@Composable
fun RecipeCard(card: DeckCard, persons: Int) {
    val r = card.recipe
    val leftover = card.leftover
    Surface(
        Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (leftover != null) yellow() else MaterialTheme.colorScheme.outline),
        tonalElevation = 2.dp, shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Text(r?.emoji ?: "🍱", fontSize = 76.sp)
            Spacer(Modifier.height(14.dp))
            Text(if (leftover != null) "Reste : ${r?.name ?: leftover.recipeId}" else r?.name ?: "",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (leftover != null) {
                    Pill(plural(leftover.portions, "part", "parts"))
                    Pill("cuisiné le " + DateTimeFormatter.ofPattern("d MMM").format(Instant.ofEpochMilli(leftover.cookedAt).atZone(ZoneId.systemDefault())))
                } else if (r != null) {
                    if (r.minutes > 0) Pill("⏱ ${r.minutes} min")
                    r.moments.forEach { m -> Pill(Moment.of(m)?.label ?: m) }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (leftover != null) {
                Text("À réchauffer, zéro ingrédient à sortir.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("→ j'en mange une part · ← plus tard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (card.coverage != null) {
                val cov = card.coverage
                GaugeRow(cov)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    if (cov.full) {
                        Text("Tout est là pour ${plural(persons, "personne", "personnes")}.", fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("Manque ${cov.missing.size}/${cov.rows.size} :", fontWeight = FontWeight.SemiBold)
                        cov.missing.take(5).forEach { m ->
                            Text("• ${m.ingredient.name} · ${formatQty(m.lack, m.ingredient.unit)}", color = bad(), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (cov.missing.size > 5) Text("… et ${cov.missing.size - 5} autres", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("toucher = détail · → garder · ← passer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Pill(text: String) {
    Text(text, Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Jauge de couverture : jaune plein = tout est en stock ; sinon remplissage bleu atténué. */
@Composable
fun GaugeRow(cov: Coverage) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearProgressIndicator(
            progress = { cov.ratio.toFloat() },
            modifier = Modifier.weight(1f).height(10.dp),
            color = if (cov.full) yellow() else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp, drawStopIndicator = {},
        )
        Text(if (cov.full) "✓ prêt" else "${cov.percent} %", fontWeight = FontWeight.SemiBold,
            color = if (cov.full) yellow() else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
    }
}
