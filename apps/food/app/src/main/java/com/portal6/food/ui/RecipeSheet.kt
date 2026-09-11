package com.portal6.food.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portal6.food.domain.FoodState
import com.portal6.food.domain.Moment
import com.portal6.food.domain.Recipe
import com.portal6.food.domain.RecipeIngredient
import com.portal6.food.domain.coverage
import com.portal6.food.domain.formatNumber
import com.portal6.food.domain.formatQty
import com.portal6.food.domain.plural
import kotlin.math.roundToInt

/** Fiche recette : slider personnes, jauge, ingrédients (lecture / ajustement), étapes, « Je cuisine ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSheet(vm: FoodViewModel, state: FoodState, recipe: Recipe, initialPersons: Int, leftoverId: Long?, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var persons by remember(recipe.id) { mutableIntStateOf(initialPersons) }
    var edit by remember(recipe.id) { mutableStateOf(false) }
    var cooking by remember(recipe.id) { mutableStateOf(false) }
    val leftover = leftoverId?.let { id -> state.leftovers.firstOrNull { it.id == id } }

    // Brouillon d'ajustement (quantités par personne), vivant seulement en mode édition.
    var draftName by remember(recipe.id) { mutableStateOf(recipe.name) }
    var draftMinutes by remember(recipe.id) { mutableStateOf(recipe.minutes.toString()) }
    val draftIngs = remember(recipe.id) { recipe.ingredients.map { it.ingredientId to it.qtyPerPerson.toString() }.toMutableStateList() }

    val shown = if (edit) recipe.copy(ingredients = draftIngs.map { (id, q) -> RecipeIngredient(id, q.toDoubleOrNull() ?: 0.0) }) else recipe
    val cov = state.coverage(shown, persons)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
            // En-tête
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.emoji, fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (edit) {
                        OutlinedTextField(draftName, { draftName = it }, singleLine = true, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(draftMinutes, { draftMinutes = it.filter { c -> c.isDigit() } }, singleLine = true, label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(120.dp))
                    } else {
                        Text(recipe.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            buildString {
                                if (recipe.minutes > 0) append("⏱ ${recipe.minutes} min · ")
                                append(recipe.moments.joinToString(", ") { Moment.of(it)?.label ?: it })
                                if (recipe.isUser) append(" · ajustée")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Fermer") }
            }

            // Personnes + jauge
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👥")
                Slider(value = persons.toFloat(), onValueChange = { persons = it.roundToInt() }, valueRange = 1f..8f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                Text(plural(persons, "personne", "personnes"), fontWeight = FontWeight.SemiBold)
            }
            GaugeRow(cov)

            // Ingrédients
            SectionTitle(if (edit) "Ingrédients (quantité par personne)" else "Ingrédients pour $persons")
            if (edit) {
                draftIngs.forEachIndexed { idx, (id, qty) ->
                    val ing = state.ingredientById[id] ?: return@forEachIndexed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ing.name, Modifier.weight(1f))
                        OutlinedTextField(qty, { v -> draftIngs[idx] = id to v.replace(',', '.') }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.width(90.dp),
                            suffix = { Text(ing.unit, style = MaterialTheme.typography.labelSmall) })
                        IconButton(onClick = { draftIngs.removeAt(idx) }) { Icon(Icons.Filled.Close, "Retirer", tint = bad()) }
                    }
                }
                AddIngredientLine(state, used = draftIngs.map { it.first }.toSet()) { id, q -> draftIngs.add(id to q) }
            } else {
                cov.rows.forEach { row ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(row.ingredient.name, Modifier.weight(1f))
                        Text(formatQty(row.need, row.ingredient.unit), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(10.dp))
                        Text(if (row.ok) "✓" else "manque ${formatQty(row.lack, row.ingredient.unit)}",
                            color = if (row.ok) ok() else bad(), style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }

            // Étapes
            if (!edit && recipe.steps.isNotEmpty()) {
                SectionTitle("Préparation")
                recipe.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", Modifier.padding(vertical = 3.dp)) }
            }

            // Je cuisine
            if (cooking) {
                Spacer(Modifier.height(12.dp))
                CookBox(cov = cov, persons = persons, onCancel = { cooking = false }) { eaten -> vm.cook(recipe.id, persons, eaten) }
            }

            // Actions
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    edit -> {
                        Button(onClick = {
                            vm.updateRecipe(recipe.id, draftName.ifBlank { recipe.name }, draftMinutes.toIntOrNull() ?: 0,
                                draftIngs.map { (id, q) -> RecipeIngredient(id, q.toDoubleOrNull() ?: 0.0) })
                            edit = false
                        }) { Text("Enregistrer") }
                        OutlinedButton(onClick = { edit = false }) { Text("Annuler") }
                    }
                    leftover != null -> {
                        Button(onClick = { vm.eatLeftover(leftover.id); onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = yellow(), contentColor = MaterialTheme.colorScheme.background)) {
                            Text("🍴 J'en mange une part (reste ${formatNumber(leftover.portions)})")
                        }
                    }
                    !cooking -> {
                        Button(onClick = { cooking = true },
                            colors = ButtonDefaults.buttonColors(containerColor = yellow(), contentColor = MaterialTheme.colorScheme.background)) { Text("🍳 Je cuisine") }
                        OutlinedButton(onClick = {
                            draftName = recipe.name; draftMinutes = recipe.minutes.toString()
                            draftIngs.clear(); draftIngs.addAll(recipe.ingredients.map { it.ingredientId to formatNumber(it.qtyPerPerson) })
                            edit = true
                        }) { Text("✏️ Ajuster") }
                        Spacer(Modifier.weight(1f))
                        if (state.isLiked(recipe.id)) TextButton(onClick = { vm.unlike(recipe.id) }) { Text("Retirer", color = bad()) }
                        else TextButton(onClick = { vm.swipe(recipe.id, true) }) { Text("♥ Garder") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), Modifier.padding(top = 18.dp, bottom = 6.dp), style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
}

@Composable
private fun AddIngredientLine(state: FoodState, used: Set<String>, onAdd: (String, String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<String?>(null) }
    var qty by remember { mutableStateOf("") }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Text(chosen?.let { state.ingredientById[it]?.name } ?: "+ ajouter un ingrédient…")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.ingredients.filter { it.id !in used }.forEach { ing ->
                DropdownMenuItem(text = { Text("${ing.name} (${ing.unit})") }, onClick = { chosen = ing.id; open = false })
            }
        }
        OutlinedTextField(qty, { qty = it.replace(',', '.') }, singleLine = true, placeholder = { Text("qté") }, modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Button(onClick = { chosen?.let { if ((qty.toDoubleOrNull() ?: 0.0) > 0) { onAdd(it, qty); chosen = null; qty = "" } } }, enabled = chosen != null) { Text("OK") }
    }
}

@Composable
private fun CookBox(cov: com.portal6.food.domain.Coverage, persons: Int, onCancel: () -> Unit, onConfirm: (Int) -> Unit) {
    var eaten by remember(persons) { mutableIntStateOf(1.coerceAtMost(persons)) }
    val rest = (persons - eaten).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Text("Je cuisine pour ${plural(persons, "personne", "personnes")}", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Parts mangées maintenant", Modifier.weight(1f))
            OutlinedButton(onClick = { eaten = (eaten - 1).coerceAtLeast(0) }) { Text("−") }
            Text("$eaten", Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = { eaten = (eaten + 1).coerceAtMost(persons) }) { Text("+") }
        }
        Spacer(Modifier.height(6.dp))
        val dec = cov.rows.joinToString(", ") { "${it.ingredient.name} −${formatQty(minOf(it.have, it.need), it.ingredient.unit)}" }
        Text("Stock décrémenté : ${dec.ifEmpty { "rien" }}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (cov.missing.isNotEmpty()) Text("⚠ ${cov.missing.size} ingrédient(s) pas en stock — décrémenté à 0, pas en négatif.", style = MaterialTheme.typography.bodySmall, color = bad())
        Text(if (rest > 0) "${plural(rest, "part", "parts")} au frigo → Validées / Restes." else "Aucun reste.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onConfirm(eaten) }, colors = ButtonDefaults.buttonColors(containerColor = yellow(), contentColor = MaterialTheme.colorScheme.background)) { Text("Confirmer") }
            OutlinedButton(onClick = onCancel) { Text("Annuler") }
        }
    }
}
