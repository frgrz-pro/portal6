package com.portal6.haremote.data

/**
 * Une pièce = un groupe de prises pilotables ensemble. Une seule pièce pour
 * l'instant (le salon) ; le mapping réel prises/lampes est encore une question
 * ouverte (cf. .docs/app-remote.md), donc le salon englobe les 8 prises.
 */
data class Room(
    val id: String,
    val label: String,
    val entityIds: List<String>,
)

object Rooms {
    const val SALON_ID = "salon"

    val Salon = Room(
        id = SALON_ID,
        label = "Salon",
        entityIds = DefaultLights.map { it.entityId },
    )
}
