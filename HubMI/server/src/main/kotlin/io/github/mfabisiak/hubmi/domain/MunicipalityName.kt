package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError

@JvmInline
value class MunicipalityName private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..100

        fun parse(
            raw: String,
            field: String,
        ): Either<FieldError, MunicipalityName> = parseText(raw, field, LENGTHS).map(::MunicipalityName)
    }
}
