package io.github.mfabisiak.hubmi.assistant

import kotlinx.serialization.json.JsonObject

/**
 * One chat turn to the model. The [schema] (JSON Schema) constrains the answer to JSON of that shape, which keeps the
 * syntax valid but not the sense: the answer still goes through validation.
 */
data class LlmRequest(
    val system: String,
    val user: String,
    val schema: JsonObject,
    val maxTokens: Int,
    val temperature: Double = DEFAULT_TEMPERATURE,
) {
    companion object {
        const val DEFAULT_TEMPERATURE = 0.2
    }
}
