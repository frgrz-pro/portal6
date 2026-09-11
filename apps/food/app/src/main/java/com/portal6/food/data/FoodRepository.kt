package com.portal6.food.data

import android.content.Context
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.portal6.food.db.FoodDatabase
import com.portal6.food.domain.FoodState
import com.portal6.food.domain.Ingredient
import com.portal6.food.domain.Leftover
import com.portal6.food.domain.Recipe
import com.portal6.food.domain.RecipeIngredient
import com.portal6.food.domain.Swipe
import com.portal6.food.domain.slug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val SEP_LIST = "|"
private const val SEP_STEPS = ""

/**
 * Accès à la DB embarquée (SQLite via SQLDelight). Une seule instance par process.
 * Lecture = flows (l'UI se met à jour toute seule), écriture = fonctions suspend en transaction.
 */
class FoodRepository(context: Context) {

    private val db = FoodDatabase(AndroidSqliteDriver(FoodDatabase.Schema, context, "food.db"))
    private val q = db.foodQueries

    init {
        seed()
    }

    /** Injecte le catalogue ; complète les manques sans jamais écraser ce que l'utilisateur a modifié. */
    private fun seed() {
        val now = System.currentTimeMillis()
        db.transaction {
            Seed.ingredients.forEachIndexed { i, ing ->
                q.insertIngredientIgnore(ing.id, ing.name, ing.unit, ing.category, i.toLong())
                q.updateIngredientSort(i.toLong(), ing.id)
                q.ensureStock(ing.id, now)
            }
            Seed.recipes.forEach { r ->
                if (q.recipeExists(r.id).executeAsOne() > 0) return@forEach
                q.insertRecipe(
                    r.id, r.name, r.emoji, r.moments.joinToString(SEP_LIST), r.minutes.toLong(),
                    r.steps.joinToString(SEP_STEPS), "seed", now,
                )
                r.ingredients.forEach { (ingId, qty) -> q.insertRecipeIngredient(r.id, ingId, qty) }
            }
        }
    }

    // --- lecture -------------------------------------------------------------------------

    val state: Flow<FoodState> = combine(
        q.selectIngredients().asFlow().mapToList(Dispatchers.IO),
        q.selectStock().asFlow().mapToList(Dispatchers.IO),
        q.selectRecipes().asFlow().mapToList(Dispatchers.IO),
        q.selectRecipeIngredients().asFlow().mapToList(Dispatchers.IO),
        q.selectSwipes().asFlow().mapToList(Dispatchers.IO),
        q.selectLeftovers().asFlow().mapToList(Dispatchers.IO),
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val ings = (arr[0] as List<com.portal6.food.db.Ingredient>).map { Ingredient(it.id, it.name, it.unit, it.category) }
        @Suppress("UNCHECKED_CAST")
        val stock = (arr[1] as List<com.portal6.food.db.Stock>).associate { it.ingredient_id to it.qty }
        @Suppress("UNCHECKED_CAST")
        val ris = (arr[3] as List<com.portal6.food.db.Recipe_ingredient>).groupBy { it.recipe_id }
        @Suppress("UNCHECKED_CAST")
        val recipes = (arr[2] as List<com.portal6.food.db.Recipe>).map { r ->
            Recipe(
                id = r.id, name = r.name, emoji = r.emoji,
                moments = r.moments.split(SEP_LIST).filter { it.isNotEmpty() },
                minutes = r.minutes.toInt(),
                steps = r.steps.split(SEP_STEPS).filter { it.isNotEmpty() },
                source = r.source,
                ingredients = ris[r.id].orEmpty().map { RecipeIngredient(it.ingredient_id, it.qty) },
            )
        }
        @Suppress("UNCHECKED_CAST")
        val swipes = (arr[4] as List<com.portal6.food.db.Swipe>).map { Swipe(it.recipe_id, it.verdict, it.at) }
        @Suppress("UNCHECKED_CAST")
        val leftovers = (arr[5] as List<com.portal6.food.db.Leftover>).map { Leftover(it.id, it.recipe_id, it.portions, it.cooked_at) }
        FoodState(ings, stock, recipes, swipes, leftovers)
    }

    // --- stock ---------------------------------------------------------------------------

    suspend fun setStock(ingredientId: String, qty: Double) = io {
        q.setStock(ingredientId, max(0.0, qty), now())
    }

    suspend fun deltaStock(ingredientId: String, delta: Double) = io {
        q.deltaStock(delta, now(), ingredientId)
    }

    suspend fun addIngredient(name: String, unit: String, category: String): String = io {
        val base = slug(name)
        var id = base
        var n = 1
        while (q.ingredientExists(id).executeAsOne() > 0) { n++; id = "${base}_$n" }
        db.transaction {
            q.insertIngredient(id, name.trim(), unit, category, q.maxIngredientSort().executeAsOne() + 1)
            q.ensureStock(id, now())
        }
        id
    }

    // --- recettes ------------------------------------------------------------------------

    /** Ajuste une recette (nom, durée, quantités par personne). Elle passe en source `user`. */
    suspend fun updateRecipe(id: String, name: String, minutes: Int, ingredients: List<RecipeIngredient>) = io {
        db.transaction {
            q.updateRecipe(name.trim(), minutes.toLong(), id)
            q.deleteRecipeIngredients(id)
            ingredients.filter { it.qtyPerPerson > 0 }.forEach { q.insertRecipeIngredient(id, it.ingredientId, it.qtyPerPerson) }
        }
    }

    // --- swipes / cuisine / restes -------------------------------------------------------

    suspend fun swipe(recipeId: String, like: Boolean) = io {
        q.upsertSwipe(recipeId, if (like) "like" else "skip", now())
    }

    suspend fun unlike(recipeId: String) = io { q.deleteSwipe(recipeId) }

    /**
     * Je cuisine : décrémente le stock pour `persons` (jamais sous 0), met les parts non mangées
     * en restes, garde l'historique et sort la recette des validées. Renvoie les parts en reste.
     */
    suspend fun cook(recipeId: String, persons: Int, eaten: Int): Double = io {
        val t = now()
        val rest = max(0.0, (persons - min(eaten, persons)).toDouble())
        db.transaction {
            q.selectIngredientsOfRecipe(recipeId).executeAsList().forEach {
                q.consumeStock(it.qty * persons, t, it.ingredient_id)
            }
            if (rest > 0) q.insertLeftover(recipeId, rest, t)
            q.insertCook(recipeId, persons.toDouble(), min(eaten, persons).toDouble(), t)
            q.deleteSwipe(recipeId)
        }
        rest
    }

    suspend fun eatLeftover(id: Long, portions: Double = 1.0) = io {
        val left = (q.leftoverPortions(id).executeAsOneOrNull() ?: return@io) - portions
        if (left <= 0.001) q.deleteLeftover(id) else q.updateLeftover(left, id)
    }

    private fun now() = System.currentTimeMillis()
    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}
