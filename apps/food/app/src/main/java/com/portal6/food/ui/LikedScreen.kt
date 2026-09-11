package com.portal6.food.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal6.food.domain.FoodState
import com.portal6.food.domain.coverage
import com.portal6.food.domain.plural
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LikedScreen(vm: FoodViewModel, state: FoodState, persons: Int) {
    LazyColumn(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Section("Restes à réchauffer") }
        if (state.leftovers.isEmpty()) item { Hint("Aucun reste. Cuisine pour plusieurs, mange une part : le reste arrive ici.") }
        items(state.leftovers, key = { "L${it.id}" }) { l ->
            val r = state.recipeById[l.recipeId]
            RowCard(onClick = r?.let { { vm.openDetail(it.id, l.id) } }) {
                Text(r?.emoji ?: "🍱", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r?.name ?: l.recipeId, fontWeight = FontWeight.SemiBold)
                    Text("${plural(l.portions, "part", "parts")} · ${DateTimeFormatter.ofPattern("dd/MM").format(Instant.ofEpochMilli(l.cookedAt).atZone(ZoneId.systemDefault()))}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { vm.eatLeftover(l.id) }) { Text("−1 part") }
            }
        }
        item { Section("Recettes validées") }
        if (state.liked.isEmpty()) item { Hint("Rien de validé. Swipe à droite dans Idées.") }
        items(state.liked, key = { it.id }) { r ->
            val cov = state.coverage(r, persons)
            RowCard(onClick = { vm.openDetail(r.id) }) {
                Text(r.emoji, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.name, fontWeight = FontWeight.SemiBold)
                    Text(if (cov.full) "✓ prêt pour $persons" else "manque ${cov.missing.size}/${cov.rows.size}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinearProgressIndicator(progress = { cov.ratio.toFloat() }, modifier = Modifier.width(70.dp).height(8.dp),
                    color = if (cov.full) yellow() else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp, drawStopIndicator = {})
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun Section(text: String) {
    Text(text.uppercase(), Modifier.padding(top = 14.dp, bottom = 2.dp), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
}

@Composable
fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun RowCard(onClick: (() -> Unit)? = null, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Surface(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
