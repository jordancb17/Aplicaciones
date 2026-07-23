package com.hematoscope.app.data.db

import androidx.compose.ui.geometry.Offset
import org.json.JSONObject

/**
 * Serialisation helpers shared by the repository. Kept as top-level functions
 * (rather than Room @TypeConverters) because the entities already store the
 * serialised strings directly, which keeps the schema explicit and portable.
 */
object Serialization {

    /** Encode a tally map (cellTypeId → count) to a JSON object string. */
    fun encodeTallies(map: Map<String, Int>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    /** Decode a JSON object string back into a tally map. */
    fun decodeTallies(json: String): Map<String, Int> {
        if (json.isBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            buildMap {
                obj.keys().forEach { key -> put(key, obj.optInt(key, 0)) }
            }
        }.getOrDefault(emptyMap())
    }

    /** Encode a list of pixel points to "x,y;x,y;...". */
    fun encodePoints(points: List<Offset>): String =
        points.joinToString(";") { "${it.x},${it.y}" }

    /** Decode "x,y;x,y;..." back to a list of points. */
    fun decodePoints(csv: String): List<Offset> {
        if (csv.isBlank()) return emptyList()
        return csv.split(";").mapNotNull { pair ->
            val parts = pair.split(",")
            val x = parts.getOrNull(0)?.toFloatOrNull()
            val y = parts.getOrNull(1)?.toFloatOrNull()
            if (x != null && y != null) Offset(x, y) else null
        }
    }
}
