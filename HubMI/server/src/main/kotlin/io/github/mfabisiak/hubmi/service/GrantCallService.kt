package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.CreateApplicationRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.models.ApplicationItem
import io.github.mfabisiak.hubmi.models.GrantCallItem
import io.github.mfabisiak.hubmi.models.computeStatus
import io.github.mfabisiak.hubmi.models.toDto
import io.github.mfabisiak.hubmi.repository.ApplicationRepository
import io.github.mfabisiak.hubmi.repository.GrantCallRepository
import io.github.mfabisiak.hubmi.repository.IdeaRepository
import org.bson.types.ObjectId
import java.time.Clock
import java.time.Instant

class GrantCallService(
    private val callRepository: GrantCallRepository,
    private val applicationRepository: ApplicationRepository,
    private val ideaRepository: IdeaRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun list(status: CallStatus?): Either<DomainError, List<GrantCallDto>> =
        either {
            val calls =
                callRepository
                    .findAll()
                    .mapLeft { it.toDomainError() }
                    .bind()
            val dtos = calls.map { it.toDto(clock) }
            if (status != null) {
                dtos.filter { it.status == status }
            } else {
                dtos
            }
        }

    suspend fun active(): Either<DomainError, List<GrantCallDto>> =
        either {
            val calls =
                callRepository
                    .findAll()
                    .mapLeft { it.toDomainError() }
                    .bind()
            calls.map { it.toDto(clock) }.filter { it.status == CallStatus.OPEN }
        }

    suspend fun getById(idString: String): Either<DomainError, GrantCallDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val call =
                ensureNotNull(
                    callRepository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: $idString")
                }
            call.toDto(clock)
        }

    suspend fun create(request: UpsertCallRequest): Either<DomainError, GrantCallDto> =
        either {
            validateCallRequest(request).bind()
            val opensAt = parseInstant(request.opensAt, "opensAt").bind()
            val closesAt = parseInstant(request.closesAt, "closesAt").bind()
            ensure(opensAt.isBefore(closesAt)) {
                DomainError.Validation(
                    message = "Data otwarcia naboru musi być wcześniejsza niż data zamknięcia",
                    details =
                        listOf(
                            FieldError(
                                field = "opensAt",
                                code = FieldErrorCode.InvalidFormat,
                                message = "Data otwarcia musi być wcześniejsza niż data zamknięcia",
                            ),
                        ),
                )
            }

            val now = clock.instant()
            val item =
                GrantCallItem(
                    title = request.title.trim(),
                    description = request.description.trim(),
                    opensAt = opensAt,
                    closesAt = closesAt,
                    fields = request.fields,
                    createdAt = now,
                    updatedAt = now,
                )

            callRepository
                .create(item)
                .mapLeft { it.toDomainError() }
                .bind()
                .toDto(clock)
        }

    suspend fun update(
        idString: String,
        request: UpsertCallRequest,
    ): Either<DomainError, GrantCallDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            validateCallRequest(request).bind()
            val opensAt = parseInstant(request.opensAt, "opensAt").bind()
            val closesAt = parseInstant(request.closesAt, "closesAt").bind()
            ensure(opensAt.isBefore(closesAt)) {
                DomainError.Validation(
                    message = "Data otwarcia naboru musi być wcześniejsza niż data zamknięcia",
                    details =
                        listOf(
                            FieldError(
                                field = "opensAt",
                                code = FieldErrorCode.InvalidFormat,
                                message = "Data otwarcia musi być wcześniejsza niż data zamknięcia",
                            ),
                        ),
                )
            }

            val existing =
                ensureNotNull(
                    callRepository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: $idString")
                }

            val updatedItem =
                existing.copy(
                    title = request.title.trim(),
                    description = request.description.trim(),
                    opensAt = opensAt,
                    closesAt = closesAt,
                    fields = request.fields,
                    updatedAt = clock.instant(),
                )

            val updated =
                ensureNotNull(
                    callRepository
                        .update(updatedItem)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: $idString")
                }

            updated.toDto(clock)
        }

    suspend fun delete(idString: String): Either<DomainError, Unit> =
        either {
            val objectId = parseObjectId(idString).bind()
            val deleted =
                callRepository
                    .deleteById(objectId)
                    .mapLeft { it.toDomainError() }
                    .bind()
            ensure(deleted) {
                DomainError.NotFound("Nie znaleziono naboru o id: $idString")
            }
        }

    suspend fun apply(
        callIdString: String,
        applicantId: String,
        request: CreateApplicationRequest,
    ): Either<DomainError, ApplicationDto> =
        either {
            val callObjectId = parseObjectId(callIdString).bind()
            val call =
                ensureNotNull(
                    callRepository
                        .findById(callObjectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: $callIdString")
                }

            ensure(call.computeStatus(clock) == CallStatus.OPEN) {
                DomainError.Conflict("Nabór nie jest obecnie otwarty")
            }

            val reqIdeaId = request.ideaId
            val ideaObjectId =
                if (reqIdeaId != null) {
                    val id = parseObjectId(reqIdeaId).bind()
                    val idea =
                        ensureNotNull(
                            ideaRepository
                                .findById(id)
                                .mapLeft { it.toDomainError() }
                                .bind(),
                        ) {
                            DomainError.NotFound("Nie znaleziono pomysłu o id: $reqIdeaId")
                        }
                    ensure(idea.authorId == applicantId) {
                        DomainError.Forbidden("Wskazany pomysł nie należy do Ciebie")
                    }
                    id
                } else {
                    null
                }

            val allowedKeys = call.fields.map { it.key }.toSet()
            val fieldErrors =
                buildList {
                    // Check required fields
                    call.fields.filter { it.required }.forEach { field ->
                        val value = request.answers[field.key]
                        if (value.isNullOrBlank()) {
                            add(
                                FieldError(
                                    field = field.key,
                                    code = FieldErrorCode.Blank,
                                    message = "Pole '${field.label}' jest wymagane",
                                ),
                            )
                        }
                    }

                    // Check unknown keys
                    request.answers.keys.forEach { key ->
                        if (key !in allowedKeys) {
                            add(
                                FieldError(
                                    field = key,
                                    code = FieldErrorCode.InvalidFormat,
                                    message = "Nieznane pole wniosku: $key",
                                ),
                            )
                        }
                    }
                }

            ensure(fieldErrors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji wniosku",
                    details = fieldErrors,
                )
            }

            val application =
                ApplicationItem(
                    callId = callObjectId,
                    applicantId = applicantId,
                    ideaId = ideaObjectId,
                    answers = request.answers,
                    createdAt = clock.instant(),
                )

            val created =
                applicationRepository
                    .create(application)
                    .mapLeft { it.toDomainError() }
                    .bind()

            created.toDto()
        }

    private fun validateCallRequest(request: UpsertCallRequest): Either<DomainError.Validation, Unit> =
        either {
            val errors =
                buildList {
                    if (request.title.isBlank()) {
                        add(FieldError("title", FieldErrorCode.Blank, "Tytuł naboru nie może być pusty"))
                    }
                    if (request.description.isBlank()) {
                        add(FieldError("description", FieldErrorCode.Blank, "Opis naboru nie może być pusty"))
                    }
                    val keys = request.fields.map { it.key }
                    if (keys.size != keys.toSet().size) {
                        add(
                            FieldError(
                                field = "fields",
                                code = FieldErrorCode.InvalidFormat,
                                message = "Klucze pól formularza muszą być unikalne",
                            ),
                        )
                    }
                }
            ensure(errors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji formularza naboru",
                    details = errors,
                )
            }
        }

    private fun parseObjectId(idString: String): Either<DomainError.Validation, ObjectId> =
        either {
            ensure(ObjectId.isValid(idString)) {
                DomainError.Validation(
                    message = "Nieprawidłowy format ID: $idString",
                    details = listOf(FieldError("id", FieldErrorCode.InvalidFormat, "Nieprawidłowy format ObjectId")),
                )
            }
            ObjectId(idString)
        }

    private fun parseInstant(
        value: String,
        fieldName: String,
    ): Either<DomainError.Validation, Instant> =
        Either.catch { Instant.parse(value) }.mapLeft {
            DomainError.Validation(
                message = "Nieprawidłowy format daty dla pola '$fieldName': $value",
                details = listOf(FieldError(fieldName, FieldErrorCode.InvalidFormat, "Oczekiwano formatu ISO-8601")),
            )
        }
}
