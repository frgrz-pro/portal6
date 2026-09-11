package com.portal6.food.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.portal6.food.domain.Category
import com.portal6.food.domain.FoodState
import com.portal6.food.domain.Ingredient
import com.portal6.food.domain.UNITS
import com.portal6.food.domain.formatNumber
import com.portal6.food.domain.stepFor
import kotlinx.coroutines.delay
import java.text.Normalizer

@Composable
fun StockScreen(vm: FoodViewModel, state: FoodState) {
    var query by rememberSaveable { mutableStateOf("") }
    var onlyInStock by rememberSaveable { mutableStateOf(false) }
    val norm = { s: String -> Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase() }
    val q = norm(query.trim())

    LazyColumn(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(query, { query = it }, singleLine = true, placeholder = { Text("Chercher un ingrédient…") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(onlyInStock, { onlyInStock = it })
                Text("Seulement ce qui est en stock", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        var any = false
        Category.entries.forEach { cat ->
            val items = state.ingredients.filter { it.category == cat.key }
                .filter { q.isEmpty() || norm(it.name).contains(q) }
                .filter { !onlyInStock || (state.stock[it.id] ?: 0.0) > 0 }
            if (items.isEmpty()) return@forEach
            any = true
            item(key = "cat-${cat.key}") { Section(cat.label) }
            items(items, key = { it.id }) { ing -> StockRow(ing, state.stock[ing.id] ?: 0.0, vm) }
        }
        if (!any) item { Hint("Rien ne correspond.") }
        item {
            Section("Ajouter un ingrédient")
            AddIngredientForm(vm)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Une ligne de stock : −, saisie directe (envoyée après 500 ms sans frappe), unité, +. */
@Composable
private fun StockRow(ing: Ingredient, qty: Double, vm: FoodViewModel) {
    var text by remember(ing.id, qty) { mutableStateOf(formatNumber(qty)) }
    var dirty by remember(ing.id) { mutableStateOf(false) }
    LaunchedEffect(text, dirty) {
        if (!dirty) return@LaunchedEffect
        delay(500)
        text.toDoubleOrNull()?.let { vm.setStock(ing.id, it) }
        dirty = false
    }
    val step = stepFor(ing.unit)
    RowCard {
        Text(ing.name, Modifier.weight(1f), fontWeight = if (qty > 0) FontWeight.SemiBold else FontWeight.Normal,
            color = if (qty > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { vm.deltaStock(ing.id, -step) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.width(38.dp)) { Text("−") }
        Spacer(Modifier.width(4.dp))
        OutlinedTextField(text, { text = it.replace(',', '.'); dirty = true }, singleLine = true, modifier = Modifier.width(84.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = MaterialTheme.typography.bodyMedium,
            suffix = { Text(ing.unit, style = MaterialTheme.typography.labelSmall) })
        Spacer(Modifier.width(4.dp))
        OutlinedButton(onClick = { vm.deltaStock(ing.id, step) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), modifier = Modifier.width(38.dp)) { Text("+") }
    }
}

@Composable
private fun AddIngredientForm(vm: FoodViewModel) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pièce") }
    var cat by remember { mutableStateOf(Category.FRIGO) }
    var unitOpen by remember { mutableStateOf(false) }
    var catOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column {
                OutlinedButton(onClick = { unitOpen = true }) { Text(unit) }
                DropdownMenu(unitOpen, { unitOpen = false }) { UNITS.forEach { u -> DropdownMenuItem({ Text(u) }, { unit = u; unitOpen = false }) } }
            }
            Column {
                OutlinedButton(onClick = { catOpen = true }) { Text(cat.label) }
                DropdownMenu(catOpen, { catOpen = false }) { Category.entries.forEach { c -> DropdownMenuItem({ Text(c.label) }, { cat = c; catOpen = false }) } }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.addIngredient(name, unit, cat.key); name = "" }, enabled = name.isNotBlank()) { Text("Ajouter") }
        }
    }
}
