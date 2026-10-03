package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError

@JvmInline
value class Summary private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..280

        fun parse(raw: String): Either<FieldError, Summary> = parseText(raw, "summary", LENGTHS).map(::Summary)
    }
}
