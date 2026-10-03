package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError

/** Free-text filter (`q`); a blank query means "no filter" and parses to `null`. */
@JvmInline
value class SearchQuery private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..100

        fun parse(raw: String?): EitherNel<FieldError, SearchQuery?> =
            when {
                raw.isNullOrBlank() -> Either.Right(null)
                else -> parseText(raw, "q", LENGTHS).asNel().map(::SearchQuery)
            }
    }
}
