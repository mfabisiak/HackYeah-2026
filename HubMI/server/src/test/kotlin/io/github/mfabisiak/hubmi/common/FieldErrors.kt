package io.github.mfabisiak.hubmi.common

import arrow.core.Either
import arrow.core.NonEmptyList
import io.github.mfabisiak.hubmi.api.FieldError
import kotlin.test.assertIs

internal fun errorsOf(result: Either<NonEmptyList<FieldError>, *>): List<FieldError> =
    assertIs<Either.Left<NonEmptyList<FieldError>>>(result).value
