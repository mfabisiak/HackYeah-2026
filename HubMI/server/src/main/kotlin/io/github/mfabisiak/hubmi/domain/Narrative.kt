package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError

/** Optional prose section of an innovation (as in ROPS' application form); blank means "not filled in". */
@JvmInline
value class Narrative private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..5000

        fun parse(
            raw: String?,
            field: String,
        ): Either<FieldError, Narrative?> =
            when {
                raw.isNullOrBlank() -> Either.Right(null)
                else -> parseText(raw, field, LENGTHS).map(::Narrative)
            }
    }
}
