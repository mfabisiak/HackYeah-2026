package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.common.parseText

/** The note an admin leaves for the author of a plan when reviewing it. */
@JvmInline
value class ReviewComment private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..1000

        fun parse(raw: String): Either<FieldError, ReviewComment> =
            parseText(raw, "comment", LENGTHS).map(::ReviewComment)
    }
}
