package io.github.mfabisiak.hubmi.tester

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.parseText

/** Optional free text of a feedback or test request; a blank text means "nothing written" and parses to `null`. */
@JvmInline
value class Comment private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..1000

        fun parse(
            raw: String?,
            field: String,
        ): Either<FieldError, Comment?> =
            when {
                raw.isNullOrBlank() -> Either.Right(null)
                else -> parseText(raw, field, LENGTHS).map(::Comment)
            }
    }
}
