package io.github.mfabisiak.hubmi.admin

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode

/** Length of the window (in calendar months) the admin trends are computed for. */
@JvmInline
value class TrendsMonths private constructor(
    val value: Int,
) {
    companion object {
        private val ALLOWED = 1..60

        fun parse(raw: Int): Either<FieldError, TrendsMonths> =
            either {
                ensure(raw in ALLOWED) {
                    FieldError(
                        field = "months",
                        code = FieldErrorCode.Range(ALLOWED.first, ALLOWED.last),
                        message = "Liczba miesięcy musi być w przedziale ${ALLOWED.first}..${ALLOWED.last}",
                    )
                }
                TrendsMonths(raw)
            }
    }
}
