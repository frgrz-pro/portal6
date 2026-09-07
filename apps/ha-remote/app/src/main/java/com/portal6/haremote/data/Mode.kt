package com.portal6.haremote.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Un **mode** = une des 4 touches d'un bouton MOES, et un des 4 boutons de la
 * rangée « Modes » de l'app. Même numéro, même effet, que l'on appuie sur le
 * mur ou dans l'app :
 * - **Mode 1** est câblé : tout allumer / tout éteindre (si une prise est
 *   allumée → tout s'éteint, sinon tout s'allume) ;
 * - **Modes 2, 3, 4** sont des scènes que l'on définit soi-même dans l'app
 *   (l'état on/off des 8 prises). Avec HA, elles vivent côté serveur
 *   (`scene.mode_n`) pour que les boutons physiques les jouent aussi.
 */
data class Mode(
    val number: Int,
    /** État des prises pour les modes 2-4 ; `null` = pas encore défini (ou mode 1). */
    val states: Map<String, Boolean>?,
) {
    val id: String get() = "mode_$number"
    val label: String get() = "Mode $number"
    val isAllToggle: Boolean get() = number == 1
    val isEditable: Boolean get() = !isAllToggle
    val isDefined: Boolean get() = isAllToggle || !states.isNullOrEmpty()

    /** Vrai si l'état courant des prises est celui du mode (mode 1 : tout allumé). */
    fun matches(lights: List<Light>): Boolean {
        if (lights.isEmpty()) return false
        if (isAllToggle) return lights.all { it.isOn }
        val s = states ?: return false
        return s.isNotEmpty() && lights.all { s[it.entityId] == it.isOn }
    }

    companion object {
        const val COUNT = 4
        fun defaults(): List<Mode> = (1..COUNT).map { Mode(it, null) }
    }
}

interface ModesRepository {
    val modes: StateFlow<List<Mode>>
    suspend fun apply(mode: Mode)

    /** Redéfinit le mode [number] (2-4) avec [states] : entityId → on/off. */
    suspend fun save(number: Int, states: Map<String, Boolean>)
}

/** Mode démo : modes 2-4 en mémoire + prefs, mode 1 via le dépôt des prises. */
class MockModesRepository(
    private val store: ConfigStore,
    private val lights: LightsRepository,
) : ModesRepository {

    private val _modes = MutableStateFlow(build(store.loadModes()))
    override val modes: StateFlow<List<Mode>> = _modes

    override suspend fun apply(mode: Mode) {
        if (mode.isAllToggle) {
            lights.setAll(!lights.lights.value.any { it.isOn })
        } else {
            mode.states?.let { lights.apply(it) }
        }
    }

    override suspend fun save(number: Int, states: Map<String, Boolean>) {
        val saved = store.loadModes() + (number to states)
        store.saveModes(saved)
        _modes.value = build(saved)
    }

    private fun build(saved: Map<Int, Map<String, Boolean>>): List<Mode> =
        Mode.defaults().map { if (it.isEditable) it.copy(states = saved[it.number]) else it }
}
