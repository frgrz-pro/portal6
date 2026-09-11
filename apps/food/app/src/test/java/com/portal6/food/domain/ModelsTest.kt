package com.portal6.food.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Logique pure : couverture, deck, formats — testable sans Android. */
class ModelsTest {
    private val oeuf = Ingredient("oeuf", "Œufs", "pièce", "frigo")
    private val lait = Ingredient("lait", "Lait", "ml", "frigo")
    private val crepes = Recipe("crepes", "Crêpes", "🥞", listOf("gouter", "soir"), 30, listOf("Fouetter"), "seed",
        listOf(RecipeIngredient("oeuf", 0.5), RecipeIngredient("lait", 150.0)))
    private val mousse = Recipe("mousse", "Mousse", "🍫", listOf("soir"), 20, emptyList(), "seed",
        listOf(RecipeIngredient("oeuf", 1.5)))

    private fun state(stock: Map<String, Double>, swipes: List<Swipe> = emptyList(), leftovers: List<Leftover> = emptyList()) =
        FoodState(listOf(oeuf, lait), stock, listOf(crepes, mousse), swipes, leftovers)

    @Test fun `couverture pleine quand tout est en stock`() {
        val cov = state(mapOf("oeuf" to 6.0, "lait" to 1000.0)).coverage(crepes, 6)
        assertTrue(cov.full)
        assertEquals(100, cov.percent)
        assertEquals(900.0, cov.rows[1].need, 1e-9)
    }

    @Test fun `couverture partielle liste le manque`() {
        val cov = state(mapOf("oeuf" to 6.0, "lait" to 100.0)).coverage(crepes, 2)
        assertFalse(cov.full)
        assertEquals(1, cov.missing.size)
        assertEquals("lait", cov.missing[0].ingredient.id)
        assertEquals(200.0, cov.missing[0].lack, 1e-9)
        assertEquals((1.0 + 100.0 / 300.0) / 2, cov.ratio, 1e-9)
    }

    @Test fun `deck restes d'abord puis tri par couverture, hors validées et non récents`() {
        val now = 1_000_000_000_000L
        val s = state(
            mapOf("oeuf" to 3.0, "lait" to 0.0),
            swipes = listOf(Swipe("x", "like", now)),
            leftovers = listOf(Leftover(7, "crepes", 4.0, now)),
        )
        val deck = s.buildDeck(Moment.SOIR, 2, emptySet(), now)
        assertEquals(listOf("L7", "mousse", "crepes"), deck.map { it.key })
        // « non » d'il y a 1 jour → caché ; d'il y a 4 jours → de retour
        val recent = s.copy(swipes = listOf(Swipe("mousse", "skip", now - 86_400_000L)))
        assertEquals(listOf("L7", "crepes"), recent.buildDeck(Moment.SOIR, 2, emptySet(), now).map { it.key })
        val old = s.copy(swipes = listOf(Swipe("mousse", "skip", now - 4 * 86_400_000L)))
        assertEquals(listOf("L7", "mousse", "crepes"), old.buildDeck(Moment.SOIR, 2, emptySet(), now).map { it.key })
        // validée → hors deck ; moment non couvert → hors deck ; reste ignoré → hors deck
        val liked = s.copy(swipes = listOf(Swipe("mousse", "like", now)))
        assertEquals(listOf("L7", "crepes"), liked.buildDeck(Moment.SOIR, 2, emptySet(), now).map { it.key })
        assertEquals(listOf("L7", "crepes"), s.buildDeck(Moment.GOUTER, 2, emptySet(), now).map { it.key })
        assertEquals(listOf("mousse", "crepes"), s.buildDeck(Moment.SOIR, 2, setOf(7L), now).map { it.key })
    }

    @Test fun `moment auto selon l'heure`() {
        assertEquals(Moment.MATIN, Moment.auto(8))
        assertEquals(Moment.MIDI, Moment.auto(12))
        assertEquals(Moment.GOUTER, Moment.auto(16))
        assertEquals(Moment.SOIR, Moment.auto(19))
        assertEquals(null, Moment.auto(2))
    }

    @Test fun `formats de quantité`() {
        assertEquals("3½", formatQty(3.5, "pièce"))
        assertEquals("¼", formatQty(0.25, "pièce"))
        assertEquals("300 ml", formatQty(300.0, "ml"))
        assertEquals("1 cas", formatQty(1.0, "cas"))
        assertEquals("2 parts", plural(2.0, "part", "parts"))
        assertEquals("1 part", plural(1.0, "part", "parts"))
        assertEquals("pates_au_thon", slug("Pâtes au thon !"))
    }
}
