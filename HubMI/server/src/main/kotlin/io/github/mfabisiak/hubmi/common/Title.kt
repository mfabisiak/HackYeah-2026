package io.github.mfabisiak.hubmi.common

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError

@JvmInline
value class Title private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 3..120

        fun parse(raw: String): Either<FieldError, Title> = parseText(raw, "title", LENGTHS).map(::Title)
    }
}
