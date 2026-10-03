package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.mapOrAccumulate
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.toNonEmptyListOrNull
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import org.bson.types.ObjectId

/** Shared building blocks of the value classes: every parser trims first, so a stored value is never padded. */
internal fun parseText(
    raw: String,
    field: String,
    lengths: IntRange,
): Either<FieldError, String> =
    either {
        val trimmed = raw.trim()
        ensure(trimmed.isNotEmpty()) { FieldError(field, FieldErrorCode.Blank, "Pole '$field' nie może być puste") }
        ensure(trimmed.length in lengths) {
            FieldError(
                field = field,
                code = FieldErrorCode.Range(lengths.first, lengths.last),
                message = "Pole '$field' musi mieć od ${lengths.first} do ${lengths.last} znaków",
            )
        }
        trimmed
    }

internal fun parseObjectId(raw: String): Either<FieldError, ObjectId> =
    either {
        ensure(ObjectId.isValid(raw)) {
            FieldError("id", FieldErrorCode.InvalidFormat, "Nieprawidłowy format identyfikatora")
        }
        ObjectId(raw)
    }

/** Drops duplicates and requires at least one element. */
internal fun <T> List<T>.toNonEmpty(
    field: String,
    message: String,
): Either<FieldError, NonEmptyList<T>> =
    either {
        ensureNotNull(distinct().toNonEmptyListOrNull()) { FieldError(field, FieldErrorCode.Required, message) }
    }

internal fun <T : Any> String?.parseOptional(parse: (String) -> Either<FieldError, T>): Either<FieldError, T?> =
    this?.let(parse) ?: Either.Right(null)

/** Parses every element, reporting each failure under `field[index]`. */
internal fun <T> List<String>.parseAll(
    field: String,
    parse: (String, String) -> Either<FieldError, T>,
): Either<NonEmptyList<FieldError>, List<T>> =
    withIndex().toList().mapOrAccumulate { (index, raw) -> parse(raw, "$field[$index]").bind() }

internal fun <A, B> Either<A, B>.asNel(): Either<NonEmptyList<A>, B> = mapLeft { nonEmptyListOf(it) }
