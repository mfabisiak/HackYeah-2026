package io.github.mfabisiak.hubmi.assistant

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** The few JSON Schema shapes that the answers of the model are constrained to. */
internal object JsonSchemas {
    val STRING = typed("string")
    val INTEGER = typed("integer")

    private fun typed(type: String): JsonObject = buildJsonObject { put("type", type) }

    /** An object whose every property is required, in the order given, which is the order the model writes them in. */
    fun objectSchema(vararg properties: Pair<String, JsonObject>): JsonObject =
        buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { properties.forEach { (name, schema) -> put(name, schema) } }
            putJsonArray("required") { properties.forEach { (name, _) -> add(JsonPrimitive(name)) } }
        }

    fun arraySchema(
        items: JsonObject,
        exactly: Int? = null,
    ): JsonObject =
        buildJsonObject {
            put("type", "array")
            exactly?.let {
                put("minItems", it)
                put("maxItems", it)
            }
            put("items", items)
        }
}
