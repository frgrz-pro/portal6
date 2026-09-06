package com.portal6.haremote.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Une « config de pièce » : un instantané nommé de l'état on/off des prises
 * d'une pièce (l'équivalent d'une scène). Rejouer une config = pousser
 * [states] sur le backend.
 *
 * C'est une notion applicative : elle survivra au passage du mock au vrai
 * client Home Assistant (les clés sont déjà des `entityId` HA).
 */
data class RoomConfig(
    val id: String,
    val roomId: String,
    val name: String,
    val states: Map<String, Boolean>,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("roomId", roomId)
        put("name", name)
        put("states", JSONObject().apply { states.forEach { (k, v) -> put(k, v) } })
    }

    companion object {
        fun fromJson(json: JSONObject): RoomConfig {
            val rawStates = json.optJSONObject("states") ?: JSONObject()
            val states = buildMap {
                rawStates.keys().forEach { key -> put(key, rawStates.optBoolean(key)) }
            }
            return RoomConfig(
                id = json.optString("id"),
                roomId = json.optString("roomId", Rooms.SALON_ID),
                name = json.optString("name"),
                states = states,
            )
        }

        fun listToJson(configs: List<RoomConfig>): String =
            JSONArray().apply { configs.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(raw: String?): List<RoomConfig> {
            if (raw.isNullOrBlank()) return emptyList()
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                array.optJSONObject(i)?.let(::fromJson)
            }
        }
    }
}
