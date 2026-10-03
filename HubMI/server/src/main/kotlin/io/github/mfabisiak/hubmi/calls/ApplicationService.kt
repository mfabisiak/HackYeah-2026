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
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import io.github.mfabisiak.hubmi.ideas.IdeaService
import org.bson.types.ObjectId
import java.time.Clock

class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val callRepository: GrantCallRepository,
    private val ideaService: IdeaService,
    private val clock: Clock = Clock.systemUTC(),
) {
    companion object {
        const val MAX_DRAFTS_PER_USER_CALL = 5
        const val MAX_TITLE_LENGTH = 300
        const val MAX_NARRATIVE_LENGTH = 5000
        const val MAX_PLAN_ITEMS = 50
        const val MAX_PARTNERS = 5
        const val MAX_ITEM_COST_GROSZE = 10_000_000

        private fun polishTargetGroupLabel(targetGroup: TargetGroup): String =
            when (targetGroup) {
                TargetGroup.SENIORS -> "Seniorzy"
                TargetGroup.YOUTH -> "Młodzież"
                TargetGroup.PEOPLE_WITH_DISABILITIES -> "Osoby z niepełnosprawnościami"
                TargetGroup.FAMILIES -> "Rodziny"
                TargetGroup.RESIDENTS -> "Mieszkańcy"
                TargetGroup.NGOS -> "Organizacje pozarządowe"
                TargetGroup.LOCAL_GOVERNMENTS -> "Samorządy lokalne"
            }
    }

    suspend fun apply(
        callIdString: String,
        applicantId: String,
        request: CreateApplicationDraftRequest?,
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

            val existingDrafts =
                applicationRepository
                    .countDraftsByCallAndApplicant(callObjectId, applicantId)
                    .mapLeft { it.toDomainError() }
                    .bind()
            ensure(existingDrafts < MAX_DRAFTS_PER_USER_CALL) {
                DomainError.Conflict("Osiągnięto limit $MAX_DRAFTS_PER_USER_CALL szkiców wniosków w tym naborze")
            }

            val ideaId = request?.ideaId
            val prefill =
                if (!ideaId.isNullOrBlank()) {
                    ideaService.getPrefill(ideaId, applicantId).bind()
                } else {
                    null
                }

            val prefillAudience =
                prefill?.targetGroups?.joinToString(", ") { polishTargetGroupLabel(it) }

            val now = clock.instant()
            val item =
                ApplicationItem(
                    callId = callObjectId,
                    applicantId = applicantId,
                    ideaId = prefill?.ideaId,
                    status = ApplicationStatus.DRAFT,
                    formVersion = RopsDeclarations.FORM_VERSION,
                    title = prefill?.title,
                    description = prefill?.essence,
                    problemDiagnosis = prefill?.essence,
                    audienceDescription = prefillAudience,
                    createdAt = now,
                    updatedAt = now,
                )

            val created =
                applicationRepository
                    .create(item)
                    .mapLeft { it.toDomainError() }
                    .bind()

            created.toDto()
        }

    suspend fun saveDraft(
        idString: String,
        callerId: String,
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

            ensure(existing.applicantId == callerId) {
                DomainError.Forbidden("Brak uprawnień do edycji tego wniosku (tylko autor może edytować szkic)")
            }

            ensure(existing.status == ApplicationStatus.DRAFT) {
                DomainError.Conflict(
                    "Nie można edytować wniosku o statusie ${existing.status} (tylko szkice mogą być edytowane)",
                )
            }

            validateDraftPayload(request).bind()

            val now = clock.instant()
            val updatedItem =
                existing.copy(
                    title = request.title?.trim(),
                    applicant = request.applicant?.toItem(),
                    description = request.description?.trim(),
                    innovativeness = request.innovativeness?.trim(),
                    problemDiagnosis = request.problemDiagnosis?.trim(),
                    socialArea = request.socialArea,
                    audienceDescription = request.audienceDescription?.trim(),
                    expectedChange = request.expectedChange?.trim(),
                    futureVision = request.futureVision?.trim(),
                    plan = request.plan?.toItem(),
                    requestedGrantAmountGrosze = request.requestedGrantAmountGrosze,
                    projectTeam = request.projectTeam?.trim(),
                    declarations = request.declarations,
                    updatedAt = now,
                )

            val updated =
                ensureNotNull(
                    applicationRepository
                        .updateDraft(updatedItem, expectedUpdatedAt = existing.updatedAt)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.Conflict("Szkic wniosku uległ zmianie w trakcie zapisu")
                }

            updated.toDto()
        }

    suspend fun submit(
        idString: String,
        callerId: String,
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

            ensure(existing.applicantId == callerId) {
                DomainError.Forbidden("Tylko autor może złożyć swój wniosek")
            }

            ensure(existing.status == ApplicationStatus.DRAFT) {
                DomainError.Conflict("Wniosek o statusie ${existing.status} został już złożony i jest niezmienny")
            }

            val call =
                ensureNotNull(
                    callRepository
                        .findById(existing.callId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: ${existing.callId}")
                }

            ensure(call.computeStatus(clock) == CallStatus.OPEN) {
                DomainError.Conflict("Nabór jest zamknięty. Nie można złożyć wniosku.")
            }

            RopsApplicationValidator.validateForSubmission(existing).bind()

            val incremented =
                callRepository
                    .tryIncrementApplicantSubmission(existing.callId, callerId, maxAllowed = 2)
                    .mapLeft { it.toDomainError() }
                    .bind()

            ensure(incremented) {
                DomainError.Conflict("Osiągnięto maksymalny limit 2 złożonych wniosków w tym naborze")
            }

            val now = clock.instant()
            val submitted =
                ensureNotNull(
                    applicationRepository
                        .submit(objectId, expectedUpdatedAt = existing.updatedAt, submittedAt = now)
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
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<ApplicationDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
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
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<ApplicationDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val callObjectId = callIdString?.let { parseObjectId(it).bind() }
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
        applicantType: ApplicantType?,
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
            RopsDeclarations.getDeclarations(applicantType)
        }

    private fun validateDraftPayload(request: SaveApplicationDraftRequest): Either<DomainError.Validation, Unit> =
        either {
            val errors = mutableListOf<FieldError>()

            val title = request.title
            if (title != null && title.length > MAX_TITLE_LENGTH) {
                errors.add(
                    FieldError(
                        "title",
                        FieldErrorCode.InvalidFormat,
                        "Tytuł nie może przekraczać $MAX_TITLE_LENGTH znaków",
                    ),
                )
            }

            val narrativeFields =
                listOf(
                    "description" to request.description,
                    "innovativeness" to request.innovativeness,
                    "problemDiagnosis" to request.problemDiagnosis,
                    "audienceDescription" to request.audienceDescription,
                    "expectedChange" to request.expectedChange,
                    "futureVision" to request.futureVision,
                    "projectTeam" to request.projectTeam,
                )
            narrativeFields.forEach { (name, value) ->
                if (value != null && value.length > MAX_NARRATIVE_LENGTH) {
                    errors.add(
                        FieldError(
                            name,
                            FieldErrorCode.InvalidFormat,
                            "Pole '$name' nie może przekraczać $MAX_NARRATIVE_LENGTH znaków",
                        ),
                    )
                }
            }

            request.plan?.let { plan ->
                if (plan.preparation.size > MAX_PLAN_ITEMS) {
                    errors.add(
                        FieldError(
                            "plan.preparation",
                            FieldErrorCode.Range(0, MAX_PLAN_ITEMS),
                            "Maksymalna liczba zadań w okresie przygotowawczym to $MAX_PLAN_ITEMS",
                        ),
                    )
                }
                if (plan.testingPhase1.size > MAX_PLAN_ITEMS) {
                    errors.add(
                        FieldError(
                            "plan.testingPhase1",
                            FieldErrorCode.Range(0, MAX_PLAN_ITEMS),
                            "Maksymalna liczba zadań w Fazie I to $MAX_PLAN_ITEMS",
                        ),
                    )
                }
                if (plan.testingPhase2.size > MAX_PLAN_ITEMS) {
                    errors.add(
                        FieldError(
                            "plan.testingPhase2",
                            FieldErrorCode.Range(0, MAX_PLAN_ITEMS),
                            "Maksymalna liczba zadań w Fazie II to $MAX_PLAN_ITEMS",
                        ),
                    )
                }
                val allItems = plan.preparation + plan.testingPhase1 + plan.testingPhase2
                allItems.forEachIndexed { idx, item ->
                    if (item.costGrosze < 0 || item.costGrosze > MAX_ITEM_COST_GROSZE) {
                        errors.add(
                            FieldError(
                                "plan.items[$idx].costGrosze",
                                FieldErrorCode.InvalidFormat,
                                "Koszt zadania musi mieścić się w przedziale 0..${MAX_ITEM_COST_GROSZE / 100} PLN",
                            ),
                        )
                    }
                }
            }

            ensure(errors.isEmpty()) {
                DomainError.Validation("Błędy rozmiaru danych szkicu", errors)
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
}
