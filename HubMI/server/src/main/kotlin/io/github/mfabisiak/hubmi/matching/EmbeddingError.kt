package io.github.mfabisiak.hubmi.matching

import io.github.mfabisiak.hubmi.common.DomainError

sealed interface EmbeddingError {
    /** The embedding service did not answer (not running, timeout, non-success status). */
    data class Unavailable(
        val cause: Throwable? = null,
        val detail: String? = null,
    ) : EmbeddingError

    /** The service answered, but not with one non-empty vector per text. */
    data class InvalidResponse(
        val detail: String,
    ) : EmbeddingError
}

fun EmbeddingError.toDomainError(): DomainError =
    DomainError.Unavailable("Wyszukiwanie semantyczne jest chwilowo niedostępne")
