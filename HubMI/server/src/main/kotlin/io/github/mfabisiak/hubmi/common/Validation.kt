package io.github.mfabisiak.hubmi.common

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError

/** Turns the field errors of a parsed request into the single validation error reported to the client. */
fun <T> EitherNel<FieldError, T>.orValidationError(message: String): Either<DomainError.Validation, T> =
    mapLeft { DomainError.Validation(message = message, details = it) }
