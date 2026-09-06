package com.portal6.remote.data

/**
 * Une prise pilotable. [entityId] correspondra à l'entité Home Assistant
 * (ex. "switch.multiprise_a_prise_1") quand le vrai backend sera branché.
 */
data class Light(
    val entityId: String,
    val label: String,
    val isOn: Boolean = false,
)

/**
 * Mapping des 8 boutons (2 colonnes × 4) : colonne gauche = multiprise A,
 * colonne droite = multiprise B. Les libellés sont des placeholders tant que
 * le mapping réel prises/lampes n'est pas connu (cf. .docs/app-remote.md).
 */
val DefaultLights = listOf(
    Light("switch.multiprise_a_prise_1", "A1"),
    Light("switch.multiprise_a_prise_2", "A2"),
    Light("switch.multiprise_a_prise_3", "A3"),
    Light("switch.multiprise_a_prise_4", "A4"),
    Light("switch.multiprise_b_prise_1", "B1"),
    Light("switch.multiprise_b_prise_2", "B2"),
    Light("switch.multiprise_b_prise_3", "B3"),
    Light("switch.multiprise_b_prise_4", "B4"),
)
