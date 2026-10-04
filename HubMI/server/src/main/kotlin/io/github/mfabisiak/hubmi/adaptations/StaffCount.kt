package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode

/** How many people an institution can assign to a service. */
@JvmInline
value class StaffCount private constructor(
    val value: Int,
) {
    companion object {
        private val PEOPLE = 0..10_000

        fun parse(raw: Int): Either<FieldError, StaffCount> =
            either {
                ensure(raw in PEOPLE) {
                    FieldError(
                        field = "staffCount",
                        code = FieldErrorCode.Range(PEOPLE.first, PEOPLE.last),
                        message = "Liczba osób musi być w przedziale od ${PEOPLE.first} do ${PEOPLE.last}",
                    )
                }
                StaffCount(raw)
            }
    }
}
