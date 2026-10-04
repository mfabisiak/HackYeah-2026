package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode

/** What an institution can spend on a pilot, in PLN. */
@JvmInline
value class Budget private constructor(
    val pln: Int,
) {
    companion object {
        private val AMOUNTS = 1_000..50_000_000

        fun parse(raw: Int): Either<FieldError, Budget> =
            either {
                ensure(raw in AMOUNTS) {
                    FieldError(
                        field = "budgetPln",
                        code = FieldErrorCode.Range(AMOUNTS.first, AMOUNTS.last),
                        message = "Budżet musi być w przedziale od ${AMOUNTS.first} do ${AMOUNTS.last} zł",
                    )
                }
                Budget(raw)
            }
    }
}
