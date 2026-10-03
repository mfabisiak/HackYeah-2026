package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError

/** A user's own words describing a problem; bounded so a request cannot feed the matcher an arbitrary amount of text. */
@JvmInline
value class NeedDescription private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 5..2000

        fun parse(raw: String): Either<FieldError, NeedDescription> =
            parseText(raw, "description", LENGTHS).map(::NeedDescription)
    }
}
