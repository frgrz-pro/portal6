package com.portal6.food.domain

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Modèle du domaine : Kotlin pur, aucune dépendance Android — réutilisable tel quel en KMP. */

enum class Moment(val key: String, val label: String) {
    MATIN("matin", "Matin"), MIDI("midi", "Midi"), GOUTER("gouter", "Goûter"),
    SOIR("soir", "Soir"), APERO("apero", "Apéro");

    companion object {
        fun of(key: String): Moment? = entries.firstOrNull { it.key == key }

        /** Moment auto selon l'heure locale ; null = « tout » (nuit). */
        fun auto(hour: Int): Moment? = when (hour) {
            in 6..10 -> MATIN
            in 11..14 -> MIDI
            in 15..17 -> GOUTER
            in 18..22 -> SOIR
            else -> null
        }
    }
}

enum class Category(val key: String, val label: String) {
    FRIGO("frigo", "🧊 Frigo"), FRAIS("frais", "🥬 Frais"),
    CONGELO("congelo", "❄️ Congélo"), PLACARD("placard", "🏺 Placard");

    companion object {
        fun of(key: String): Category = entries.firstOrNull { it.key == key } ?: PLACARD
    }
}

val UNITS = listOf("pièce", "g", "ml", "tranche", "cas", "cac")

/** Pas des boutons +/− du stock, par unité. */
fun stepFor(unit: String): Double = when (unit) {
    "g" -> 50.0
    "ml" -> 100.0
    else -> 1.0
}

data class Ingredient(val id: String, val name: String, val unit: String, val category: String)

data class RecipeIngredient(val ingredientId: String, val qtyPerPerson: Double)

data class Recipe(
    val id: String,
    val name: String,
    val emoji: String,
    val moments: List<String>,
    val minutes: Int,
    val steps: List<String>,
    val source: String,
    val ingredients: List<RecipeIngredient>,
) {
    val isUser: Boolean get() = source == "user"
}

data class Swipe(val recipeId: String, val verdict: String, val at: Long) {
    val isLike: Boolean get() = verdict == "like"
}

data class Leftover(val id: Long, val recipeId: String, val portions: Double, val cookedAt: Long)

data class FoodState(
    val ingredients: List<Ingredient> = emptyList(),
    val stock: Map<String, Double> = emptyMap(),
    val recipes: List<Recipe> = emptyList(),
    val swipes: List<Swipe> = emptyList(),
    val leftovers: List<Leftover> = emptyList(),
) {
    val ingredientById: Map<String, Ingredient> by lazy { ingredients.associateBy { it.id } }
    val recipeById: Map<String, Recipe> by lazy { recipes.associateBy { it.id } }
    val liked: List<Recipe> get() = swipes.filter { it.isLike }.mapNotNull { recipeById[it.recipeId] }
    fun isLiked(recipeId: String) = swipes.any { it.recipeId == recipeId && it.isLike }
}

// --- couverture du stock ------------------------------------------------------------

data class CoverageRow(
    val ingredient: Ingredient,
    val need: Double,
    val have: Double,
) {
    val ratio: Double get() = if (need > 0) min(1.0, have / need) else 1.0
    val ok: Boolean get() = have + 1e-9 >= need
    val lack: Double get() = max(0.0, need - have)
}

data class Coverage(val rows: List<CoverageRow>) {
    val ratio: Double get() = if (rows.isEmpty()) 1.0 else rows.sumOf { it.ratio } / rows.size
    val missing: List<CoverageRow> get() = rows.filter { !it.ok }
    val full: Boolean get() = missing.isEmpty()
    val percent: Int get() = (ratio * 100).roundToInt()
}

fun FoodState.coverage(recipe: Recipe, persons: Int): Coverage = Coverage(
    recipe.ingredients.map { ri ->
        val ing = ingredientById[ri.ingredientId] ?: Ingredient(ri.ingredientId, ri.ingredientId, "", "placard")
        CoverageRow(ing, ri.qtyPerPerson * persons, stock[ri.ingredientId] ?: 0.0)
    },
)

// --- le deck -----------------------------------------------------------------------

const val SKIP_DAYS = 3
private const val DAY_MS = 86_400_000L

data class DeckCard(
    val key: String,
    val recipe: Recipe?,
    val leftover: Leftover? = null,
    val coverage: Coverage? = null,
)

/** Restes d'abord, puis recettes du moment triées par couverture, hors validées et « non » récents. */
fun FoodState.buildDeck(moment: Moment?, persons: Int, skippedLeftovers: Set<Long>, now: Long): List<DeckCard> {
    val swipeById = swipes.associateBy { it.recipeId }
    val rest = leftovers.filter { it.id !in skippedLeftovers }
        .map { DeckCard("L${it.id}", recipeById[it.recipeId], leftover = it) }
    val ideas = recipes
        .filter { moment == null || moment.key in it.moments }
        .filter { r ->
            val s = swipeById[r.id] ?: return@filter true
            !s.isLike && now - s.at > SKIP_DAYS * DAY_MS
        }
        .map { DeckCard(it.id, it, coverage = coverage(it, persons)) }
        .sortedWith(
            compareByDescending<DeckCard> { it.coverage!!.ratio }
                .thenBy { it.coverage!!.missing.size }
                .thenBy { it.recipe!!.name },
        )
    return rest + ideas
}

// --- formats -----------------------------------------------------------------------

private val FRACTIONS = mapOf(0.25 to "¼", 0.5 to "½", 0.75 to "¾")

fun formatQty(q: Double, unit: String): String {
    if (unit == "g" || unit == "ml") return "${q.roundToInt()} $unit"
    val whole = floor(q).toInt()
    val frac = ((q - whole) * 4).roundToInt() / 4.0
    var s = if (whole > 0) whole.toString() else ""
    if (frac > 0) s += FRACTIONS[frac] ?: frac.toString().removePrefix("0")
    if (s.isEmpty()) s = ((q * 100).roundToInt() / 100.0).toString()
    return if (unit == "pièce") s else "$s $unit"
}

fun formatNumber(q: Double): String =
    if (q == floor(q)) q.toInt().toString() else ((q * 100).roundToInt() / 100.0).toString()

fun plural(n: Number, one: String, many: String): String {
    val v = n.toDouble()
    val txt = formatNumber(v)
    return "$txt ${if (v > 1) many else one}"
}

fun slug(s: String): String {
    val base = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    return base.ifEmpty { "x" }
}
