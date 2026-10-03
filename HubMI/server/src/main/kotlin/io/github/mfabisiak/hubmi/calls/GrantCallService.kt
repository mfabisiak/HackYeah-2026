package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.DeclarationsResponse
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.ideas.IdeaRepository
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
        request: CreateApplicationDraftRequest,
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
            val (ideaObjectId, prefillTitle, prefillDesc, prefillAudience) =
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
                    val audience = idea.targetGroups.joinToString(", ") { it.name }
                    listOf(id, idea.title, idea.essence, audience)
                } else {
                    listOf(null, null, null, null)
                }

            val now = clock.instant()
            val draft =
                ApplicationItem(
                    callId = callObjectId,
                    applicantId = applicantId,
                    ideaId = ideaObjectId as? ObjectId,
                    status = ApplicationStatus.DRAFT,
                    formVersion = RopsDeclarations.FORM_VERSION,
                    title = prefillTitle as? String,
                    description = prefillDesc as? String,
                    audienceDescription = prefillAudience as? String,
                    createdAt = now,
                    updatedAt = now,
                )

            val created =
                applicationRepository
                    .create(draft)
                    .mapLeft { it.toDomainError() }
                    .bind()

            created.toDto()
        }

    suspend fun saveDraft(
        idString: String,
        callerId: String,
        isAdmin: Boolean,
        request: SaveApplicationDraftRequest,
    ): Either<DomainError, ApplicationDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val existing =
                ensureNotNull(
                    applicationRepository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono wniosku o id: $idString")
                }

            ensure(existing.applicantId == callerId || isAdmin) {
                DomainError.Forbidden("Brak uprawnień do edycji tego wniosku")
            }

            ensure(existing.status == ApplicationStatus.DRAFT) {
                DomainError.Conflict("Wniosek został już złożony i nie może być modyfikowany")
            }

            val updated =
                existing.copy(
                    title = request.title ?: existing.title,
                    applicant = request.applicant ?: existing.applicant,
                    description = request.description ?: existing.description,
                    innovativeness = request.innovativeness ?: existing.innovativeness,
                    problemDiagnosis = request.problemDiagnosis ?: existing.problemDiagnosis,
                    socialArea = request.socialArea ?: existing.socialArea,
                    audienceDescription = request.audienceDescription ?: existing.audienceDescription,
                    expectedChange = request.expectedChange ?: existing.expectedChange,
                    futureVision = request.futureVision ?: existing.futureVision,
                    plan = request.plan ?: existing.plan,
                    requestedGrantAmountGrosze =
                        request.requestedGrantAmountGrosze ?: existing.requestedGrantAmountGrosze,
                    projectTeam = request.projectTeam ?: existing.projectTeam,
                    declarations =
                        if (request.declarations.isNotEmpty()) {
                            request.declarations
                        } else {
                            existing.declarations
                        },
                    updatedAt = clock.instant(),
                )

            val saved =
                ensureNotNull(
                    applicationRepository
                        .updateDraft(updated)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.Conflict("Wniosek został zmieniony lub złożony w międzyczasie")
                }

            saved.toDto()
        }

    suspend fun submit(
        idString: String,
        callerId: String,
        isAdmin: Boolean,
    ): Either<DomainError, ApplicationDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val existing =
                ensureNotNull(
                    applicationRepository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono wniosku o id: $idString")
                }

            ensure(existing.applicantId == callerId || isAdmin) {
                DomainError.Forbidden("Brak uprawnień do złożenia tego wniosku")
            }

            ensure(existing.status == ApplicationStatus.DRAFT) {
                DomainError.Conflict("Wniosek został już złożony")
            }

            val call =
                ensureNotNull(
                    callRepository
                        .findById(existing.callId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: ${existing.callId.toHexString()}")
                }

            ensure(call.computeStatus(clock) == CallStatus.OPEN) {
                DomainError.Conflict("Nabór jest zamknięty. Nie można złożyć wniosku.")
            }

            // Enforce limit: max 2 submitted applications per caller in this call
            val submittedCount =
                applicationRepository
                    .countSubmittedByCallAndApplicant(existing.callId, existing.applicantId)
                    .mapLeft { it.toDomainError() }
                    .bind()

            ensure(submittedCount < 2) {
                DomainError.Conflict("Osiągnięto limit maksymalnie 2 złożonych wniosków w tym naborze")
            }

            // Full validation of all 12 points
            RopsApplicationValidator.validateForSubmission(existing).bind()

            // Atomically change status to SUBMITTED
            val submitted =
                ensureNotNull(
                    applicationRepository
                        .submit(objectId, clock.instant())
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.Conflict("Nie udało się złożyć wniosku (stan uległ zmianie)")
                }

            submitted.toDto()
        }

    suspend fun getApplicationById(
        idString: String,
        callerId: String,
        isAdmin: Boolean,
    ): Either<DomainError, ApplicationDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val app =
                ensureNotNull(
                    applicationRepository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono wniosku o id: $idString")
                }

            ensure(app.applicantId == callerId || isAdmin) {
                DomainError.Forbidden("Brak uprawnień do odczytu tego wniosku")
            }

            app.toDto()
        }

    suspend fun listMyApplications(
        callerId: String,
        page: Int,
        size: Int,
    ): Either<DomainError, Page<ApplicationDto>> =
        either {
            val pageRequest = PageRequest(page.coerceAtLeast(0), size.coerceIn(1, PageRequest.MAX_SIZE))
            val result =
                applicationRepository
                    .findByApplicantId(callerId, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()

            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun adminListApplications(
        callIdString: String?,
        status: ApplicationStatus?,
        page: Int,
        size: Int,
    ): Either<DomainError, Page<ApplicationDto>> =
        either {
            val callObjectId = callIdString?.let { parseObjectId(it).bind() }
            val pageRequest = PageRequest(page.coerceAtLeast(0), size.coerceIn(1, PageRequest.MAX_SIZE))
            val result =
                applicationRepository
                    .findAll(callObjectId, status, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()

            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getDeclarations(
        callIdString: String,
        applicantTypeString: String?,
    ): Either<DomainError, DeclarationsResponse> =
        either {
            val callObjectId = parseObjectId(callIdString).bind()
            ensureNotNull(
                callRepository
                    .findById(callObjectId)
                    .mapLeft { it.toDomainError() }
                    .bind(),
            ) {
                DomainError.NotFound("Nie znaleziono naboru o id: $callIdString")
            }

            val applicantType =
                applicantTypeString?.let {
                    Either
                        .catch { ApplicantType.valueOf(it.uppercase()) }
                        .getOrNull()
                }

            RopsDeclarations.getDeclarations(applicantType)
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
