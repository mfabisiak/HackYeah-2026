package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.PageRequest

fun validatePageRequest(
    page: Int?,
    size: Int?,
): Either<DomainError.Validation, PageRequest> =
    either {
        val p = page ?: PageRequest.DEFAULT_PAGE
        val s = size ?: PageRequest.DEFAULT_SIZE
        ensure(p >= 0) {
            DomainError.Validation(
                message = "Parametr 'page' musi być >= 0 (podano: $p)",
                details = listOf(FieldError("page", "min_value", "Wartość musi być >= 0")),
            )
        }
        ensure(s in 1..PageRequest.MAX_SIZE) {
            val max = PageRequest.MAX_SIZE
            DomainError.Validation(
                message = "Parametr 'size' musi być w przedziale 1..$max (podano: $s)",
                details = listOf(FieldError("size", "range", "Wartość musi być w przedziale 1..$max")),
            )
        }
        PageRequest(page = p, size = s)
    }
