package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import org.bson.types.ObjectId

fun parseObjectId(idString: String): Either<DomainError.Validation, ObjectId> =
    either {
        ensure(ObjectId.isValid(idString)) {
            DomainError.Validation(
                message = "Nieprawidłowy format ID: $idString",
                details = listOf(FieldError("id", FieldErrorCode.InvalidFormat, "Nieprawidłowy format ObjectId")),
            )
        }
        ObjectId(idString)
    }
