package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.InnovationSummary
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.models.InnovationItem
import io.github.mfabisiak.hubmi.models.toDto
import io.github.mfabisiak.hubmi.models.toSummary
import io.github.mfabisiak.hubmi.repository.InnovationRepository
import java.time.Instant

class InnovationService(
    private val repository: InnovationRepository,
) {
    suspend fun list(
        q: String?,
        area: SocialArea?,
        targetGroup: TargetGroup?,
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<InnovationSummary>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val result =
                repository
                    .findAll(q, area, targetGroup, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toSummary() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(idString: String): Either<DomainError, InnovationDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val item =
                ensureNotNull(
                    repository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono innowacji o ID: $idString")
                }
            item.toDto()
        }

    suspend fun create(request: UpsertInnovationRequest): Either<DomainError, InnovationDto> =
        either {
            validateRequest(request).bind()
            val now = Instant.now().toString()
            val item =
                InnovationItem(
                    title = request.title.trim(),
                    summary = request.summary.trim(),
                    description = request.description.trim(),
                    areas = request.areas,
                    targetGroups = request.targetGroups,
                    stage = request.stage,
                    region = request.region?.trim(),
                    mediaUrls = request.mediaUrls.map { it.trim() },
                    createdAt = now,
                    updatedAt = now,
                )
            repository
                .create(item)
                .mapLeft { it.toDomainError() }
                .bind()
                .toDto()
        }

    suspend fun update(
        idString: String,
        request: UpsertInnovationRequest,
    ): Either<DomainError, InnovationDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            validateRequest(request).bind()
            val now = Instant.now().toString()
            val updated =
                ensureNotNull(
                    repository
                        .update(objectId, request, now)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono innowacji o ID: $idString")
                }
            updated.toDto()
        }

    suspend fun delete(idString: String): Either<DomainError, Unit> =
        either {
            val objectId = parseObjectId(idString).bind()
            val now = Instant.now().toString()
            val deleted =
                repository
                    .softDelete(objectId, now)
                    .mapLeft { it.toDomainError() }
                    .bind()
            ensure(deleted) {
                DomainError.NotFound("Nie znaleziono innowacji o ID: $idString do usunięcia")
            }
        }

    private fun validateRequest(request: UpsertInnovationRequest): Either<DomainError.Validation, Unit> =
        either {
            val errors =
                buildList {
                    if (request.title.isBlank()) {
                        add(FieldError("title", FieldErrorCode.Blank, "Tytuł nie może być pusty"))
                    } else if (request.title.trim().length !in 3..120) {
                        add(FieldError("title", FieldErrorCode.Range(3, 120), "Tytuł musi mieć od 3 do 120 znaków"))
                    }

                    if (request.summary.isBlank()) {
                        add(FieldError("summary", FieldErrorCode.Blank, "Podsumowanie nie może być puste"))
                    } else if (request.summary.trim().length !in 1..280) {
                        add(
                            FieldError(
                                "summary",
                                FieldErrorCode.Range(1, 280),
                                "Podsumowanie nie może przekraczać 280 znaków",
                            ),
                        )
                    }

                    if (request.description.isBlank()) {
                        add(FieldError("description", FieldErrorCode.Blank, "Opis nie może być pusty"))
                    }

                    if (request.areas.isEmpty()) {
                        add(
                            FieldError(
                                "areas",
                                FieldErrorCode.Required,
                                "Wymagany jest co najmniej jeden obszar społeczny",
                            ),
                        )
                    }

                    if (request.targetGroups.isEmpty()) {
                        add(
                            FieldError(
                                "targetGroups",
                                FieldErrorCode.Required,
                                "Wymagana jest co najmniej jedna grupa docelowa",
                            ),
                        )
                    }

                    request.mediaUrls.forEachIndexed { index, url ->
                        val trimmed = url.trim()
                        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                            add(
                                FieldError(
                                    "mediaUrls[$index]",
                                    FieldErrorCode.InvalidFormat,
                                    "Niepoprawny format adresu URL",
                                ),
                            )
                        }
                    }
                }

            ensure(errors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji danych innowacji",
                    details = errors,
                )
            }
        }
}
