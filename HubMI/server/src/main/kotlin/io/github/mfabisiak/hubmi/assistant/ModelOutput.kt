package io.github.mfabisiak.hubmi.assistant

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlinx.serialization.json.Json

/** What every check of the model's JSON answers starts with: reading it as a type and a field as bounded text. */
internal object ModelOutput {
    val json = Json { ignoreUnknownKeys = true }

    inline fun <reified T> decode(raw: String): Either<ModelOutputError, T> =
        Either.catch { json.decodeFromString<T>(raw) }.mapLeft { ModelOutputError.Malformed(it.message.orEmpty()) }

    fun text(
        field: String,
        value: String,
        max: Int,
    ): Either<ModelOutputError, String> =
        either {
            val trimmed = value.trim()
            ensure(trimmed.isNotEmpty()) { ModelOutputError.BlankField(field) }
            ensure(trimmed.length <= max) { ModelOutputError.TooLong(field, max) }
            trimmed
        }
}
