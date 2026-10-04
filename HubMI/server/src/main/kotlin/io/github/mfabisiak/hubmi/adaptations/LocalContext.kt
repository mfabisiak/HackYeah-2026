package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.parseText

/** The local conditions of an institution in the caller's words; it reaches the model, so it is bounded. */
@JvmInline
value class LocalContext private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..600

        fun parse(raw: String): Either<FieldError, LocalContext> =
            parseText(raw, "context", LENGTHS).map(::LocalContext)
    }
}
