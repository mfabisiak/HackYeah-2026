package io.github.mfabisiak.hubmi.tester

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode

/** Star rating of an innovation, 1 to 5. */
@JvmInline
value class Rating private constructor(
    val value: Int,
) {
    companion object {
        private val STARS = 1..5

        fun parse(raw: Int): Either<FieldError, Rating> =
            either {
                ensure(raw in STARS) {
                    FieldError(
                        field = "rating",
                        code = FieldErrorCode.Range(STARS.first, STARS.last),
                        message = "Ocena musi być w przedziale od ${STARS.first} do ${STARS.last}",
                    )
                }
                Rating(raw)
            }
    }
}
