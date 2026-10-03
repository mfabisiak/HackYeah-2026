package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.PageRequest

fun validatePageRequest(
    page: Int?,
    size: Int?,
): Either<DomainError.ValidationFailed, PageRequest> =
    either {
        val p = page ?: PageRequest.DEFAULT_PAGE
        val s = size ?: PageRequest.DEFAULT_SIZE
        ensure(p >= 0) {
            DomainError.ValidationFailed("Parametr 'page' musi być >= 0 (podano: $p)")
        }
        ensure(s in 1..PageRequest.MAX_SIZE) {
            val max = PageRequest.MAX_SIZE
            DomainError.ValidationFailed("Parametr 'size' musi być w przedziale 1..$max (podano: $s)")
        }
        PageRequest(page = p, size = s)
    }
