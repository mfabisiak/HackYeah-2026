package io.github.mfabisiak.hubmi.innovations

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.parseText

@JvmInline
value class Region private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..120

        fun parse(raw: String): Either<FieldError, Region> = parseText(raw, "region", LENGTHS).map(::Region)
    }
}
