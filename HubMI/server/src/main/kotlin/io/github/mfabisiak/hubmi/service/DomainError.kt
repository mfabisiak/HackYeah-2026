package io.github.mfabisiak.hubmi.service

import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.repository.RepositoryError

sealed interface DomainError {
    val code: String
    val message: String

    data class NotFound(
        override val message: String,
        override val code: String = "not_found",
    ) : DomainError

    data class Conflict(
        override val message: String,
        override val code: String = "conflict",
    ) : DomainError

    data class Validation(
        override val message: String,
        val details: List<FieldError> = emptyList(),
        override val code: String = "validation_failed",
    ) : DomainError

    data class Unauthorized(
        override val message: String = "Wymagane uwierzytelnienie",
        override val code: String = "unauthorized",
    ) : DomainError

    data class Forbidden(
        override val message: String = "Brak uprawnień do wykonania tej operacji",
        override val code: String = "forbidden",
    ) : DomainError

    data class Unavailable(
        override val message: String = "Usługa chwilowo niedostępna",
        override val code: String = "service_unavailable",
    ) : DomainError

    data class Internal(
        override val message: String = "Wystąpił wewnętrzny błąd serwera",
        val cause: Throwable? = null,
        override val code: String = "internal_error",
    ) : DomainError
}

typealias ValidationFailed = DomainError.Validation

fun RepositoryError.toDomainError(): DomainError =
    when (this) {
        is RepositoryError.Conflict -> DomainError.Conflict(cause.message ?: "Konflikt danych")
        is RepositoryError.DatabaseException -> DomainError.Internal(message = "Błąd bazy danych", cause = cause)
    }
