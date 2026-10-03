package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.common.asNel
import java.time.Instant

data class CallDraft(
    val title: String,
    val description: String,
    val opensAt: Instant,
    val closesAt: Instant,
    val fields: List<CallField>,
) {
    companion object {
        private const val MAX_TITLE_LENGTH = 200
        private const val MAX_DESCRIPTION_LENGTH = 5000

        fun parse(request: UpsertCallRequest): EitherNel<FieldError, CallDraft> =
            Either.zipOrAccumulate(
                validateTitle(request.title).asNel(),
                validateDescription(request.description).asNel(),
                validateDates(request.opensAt, request.closesAt).asNel(),
            ) { title, description, (opensAt, closesAt) ->
                CallDraft(
                    title = title,
                    description = description,
                    opensAt = opensAt,
                    closesAt = closesAt,
                    fields = request.fields,
                )
            }

        private fun validateTitle(raw: String): Either<FieldError, String> =
            either {
                val trimmed = raw.trim()
                ensure(trimmed.isNotBlank()) {
                    FieldError("title", FieldErrorCode.Blank, "Tytuł naboru nie może być pusty")
                }
                ensure(trimmed.length <= MAX_TITLE_LENGTH) {
                    FieldError(
                        "title",
                        FieldErrorCode.InvalidFormat,
                        "Tytuł naboru nie może przekraczać $MAX_TITLE_LENGTH znaków",
                    )
                }
                trimmed
            }

        private fun validateDescription(raw: String): Either<FieldError, String> =
            either {
                val trimmed = raw.trim()
                ensure(trimmed.isNotBlank()) {
                    FieldError("description", FieldErrorCode.Blank, "Opis naboru nie może być pusty")
                }
                ensure(trimmed.length <= MAX_DESCRIPTION_LENGTH) {
                    FieldError(
                        "description",
                        FieldErrorCode.InvalidFormat,
                        "Opis naboru nie może przekraczać $MAX_DESCRIPTION_LENGTH znaków",
                    )
                }
                trimmed
            }

        private fun validateDates(
            opensAtRaw: String,
            closesAtRaw: String,
        ): Either<FieldError, Pair<Instant, Instant>> =
            either {
                val opensAt =
                    try {
                        Instant.parse(opensAtRaw)
                    } catch (_: Exception) {
                        raise(
                            FieldError(
                                "opensAt",
                                FieldErrorCode.InvalidFormat,
                                "Nieprawidłowy format daty otwarcia naboru (ISO-8601)",
                            ),
                        )
                    }
                val closesAt =
                    try {
                        Instant.parse(closesAtRaw)
                    } catch (_: Exception) {
                        raise(
                            FieldError(
                                "closesAt",
                                FieldErrorCode.InvalidFormat,
                                "Nieprawidłowy format daty zamknięcia naboru (ISO-8601)",
                            ),
                        )
                    }
                ensure(opensAt.isBefore(closesAt)) {
                    FieldError(
                        "opensAt",
                        FieldErrorCode.InvalidFormat,
                        "Data otwarcia naboru musi być wcześniejsza niż data zamknięcia",
                    )
                }
                opensAt to closesAt
            }
    }
}
