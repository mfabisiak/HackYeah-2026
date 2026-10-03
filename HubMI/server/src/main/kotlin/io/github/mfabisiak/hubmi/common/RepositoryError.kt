package io.github.mfabisiak.hubmi.common

sealed interface RepositoryError {
    val cause: Throwable

    data class DatabaseException(
        override val cause: Throwable,
    ) : RepositoryError

    data class Conflict(
        override val cause: Throwable,
    ) : RepositoryError
}
