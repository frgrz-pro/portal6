package com.portal6.food.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.portal6.food.FoodApp
import com.portal6.food.domain.FoodState
import com.portal6.food.domain.Moment
import com.portal6.food.domain.RecipeIngredient
import com.portal6.food.domain.plural
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Le choix de moment de l'onglet Idées : auto (heure), un moment forcé, ou tout. */
sealed class MomentChoice {
    data object Auto : MomentChoice()
    data object All : MomentChoice()
    data class Forced(val moment: Moment) : MomentChoice()
}

class FoodViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as FoodApp).repository
    private val prefs = app.getSharedPreferences("food", Context.MODE_PRIVATE)

    val state: StateFlow<FoodState> = repo.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FoodState())

    private val _persons = MutableStateFlow(prefs.getInt("persons", 2))
    val persons: StateFlow<Int> = _persons.asStateFlow()

    private val _moment = MutableStateFlow<MomentChoice>(MomentChoice.Auto)
    val moment: StateFlow<MomentChoice> = _moment.asStateFlow()

    /** Restes « plus tard » : ignorés jusqu'au prochain lancement. */
    private val _skippedLeftovers = MutableStateFlow<Set<Long>>(emptySet())
    val skippedLeftovers: StateFlow<Set<Long>> = _skippedLeftovers.asStateFlow()

    /** Recette ouverte dans la fiche (null = fermée) ; le reste associé si on vient d'une carte reste. */
    private val _detail = MutableStateFlow<Pair<String, Long?>?>(null)
    val detail: StateFlow<Pair<String, Long?>?> = _detail.asStateFlow()

    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun setPersons(n: Int) {
        _persons.value = n.coerceIn(1, 8)
        prefs.edit().putInt("persons", _persons.value).apply()
    }

    fun setMoment(choice: MomentChoice) { _moment.value = choice }

    fun openDetail(recipeId: String, leftoverId: Long? = null) { _detail.value = recipeId to leftoverId }
    fun closeDetail() { _detail.value = null }

    fun swipe(recipeId: String, like: Boolean) = viewModelScope.launch {
        repo.swipe(recipeId, like)
        val name = state.value.recipeById[recipeId]?.name ?: recipeId
        toasts.tryEmit(if (like) "$name → Validées" else "Passée pour 3 jours")
    }

    fun unlike(recipeId: String) = viewModelScope.launch {
        repo.unlike(recipeId)
        toasts.tryEmit("Retirée des validées")
    }

    fun skipLeftover(id: Long) { _skippedLeftovers.value = _skippedLeftovers.value + id }

    fun eatLeftover(id: Long) = viewModelScope.launch {
        repo.eatLeftover(id)
        toasts.tryEmit("Bon appétit — 1 part de moins")
    }

    fun cook(recipeId: String, persons: Int, eaten: Int) = viewModelScope.launch {
        val rest = repo.cook(recipeId, persons, eaten)
        toasts.tryEmit(if (rest > 0) "Stock mis à jour · ${plural(rest, "part", "parts")} en reste" else "Stock mis à jour")
        closeDetail()
    }

    fun updateRecipe(id: String, name: String, minutes: Int, ingredients: List<RecipeIngredient>) = viewModelScope.launch {
        repo.updateRecipe(id, name, minutes, ingredients)
        toasts.tryEmit("Recette ajustée")
    }

    fun setStock(ingredientId: String, qty: Double) = viewModelScope.launch { repo.setStock(ingredientId, qty) }
    fun deltaStock(ingredientId: String, delta: Double) = viewModelScope.launch { repo.deltaStock(ingredientId, delta) }

    fun addIngredient(name: String, unit: String, category: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        repo.addIngredient(name, unit, category)
        toasts.tryEmit("Ingrédient ajouté")
    }
}
