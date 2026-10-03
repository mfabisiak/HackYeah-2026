package io.github.mfabisiak.hubmi.common

import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.FieldError

sealed interface DomainError {
    val code: ErrorCode
    val message: String

    data class NotFound(
        override val message: String,
        override val code: ErrorCode = ErrorCode.NOT_FOUND,
    ) : DomainError

    data class Conflict(
        override val message: String,
        override val code: ErrorCode = ErrorCode.CONFLICT,
    ) : DomainError

    data class Validation(
        override val message: String,
        val details: List<FieldError> = emptyList(),
        override val code: ErrorCode = ErrorCode.VALIDATION_FAILED,
    ) : DomainError

    data class Unauthorized(
        override val message: String = "Wymagane uwierzytelnienie",
        override val code: ErrorCode = ErrorCode.UNAUTHORIZED,
    ) : DomainError

    data class Forbidden(
        override val message: String = "Brak uprawnień do wykonania tej operacji",
        override val code: ErrorCode = ErrorCode.FORBIDDEN,
    ) : DomainError

    data class Unavailable(
        override val message: String = "Usługa chwilowo niedostępna",
        override val code: ErrorCode = ErrorCode.SERVICE_UNAVAILABLE,
    ) : DomainError

    data class Internal(
        override val message: String = "Wystąpił wewnętrzny błąd serwera",
        val cause: Throwable? = null,
        override val code: ErrorCode = ErrorCode.INTERNAL_ERROR,
    ) : DomainError
}

fun RepositoryError.toDomainError(): DomainError =
    when (this) {
        is RepositoryError.Conflict -> DomainError.Conflict(cause.message ?: "Konflikt danych")
        is RepositoryError.DatabaseException -> DomainError.Internal(message = "Błąd bazy danych", cause = cause)
    }
