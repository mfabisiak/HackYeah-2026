package io.github.mfabisiak.hubmi.common

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError

@JvmInline
value class Description private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..5000

        fun parse(raw: String): Either<FieldError, Description> =
            parseText(raw, "description", LENGTHS).map(::Description)
    }
}
