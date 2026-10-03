package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.models.ChallengeItem
import io.github.mfabisiak.hubmi.models.toDto
import io.github.mfabisiak.hubmi.repository.ChallengeRepository
import java.time.Instant

class ChallengeService(
    private val repository: ChallengeRepository,
) {
    suspend fun list(
        area: SocialArea?,
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<ChallengeDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val result =
                repository
                    .findAll(area, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(idString: String): Either<DomainError, ChallengeDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val item =
                ensureNotNull(
                    repository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono wyzwania o ID: $idString")
                }
            item.toDto()
        }

    suspend fun create(request: UpsertChallengeRequest): Either<DomainError, ChallengeDto> =
        either {
            validateRequest(request).bind()
            val now = Instant.now().toString()
            val item =
                ChallengeItem(
                    title = request.title.trim(),
                    description = request.description.trim(),
                    area = request.area,
                    municipalities = request.municipalities.map { it.trim() },
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
        request: UpsertChallengeRequest,
    ): Either<DomainError, ChallengeDto> =
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
                    DomainError.NotFound("Nie znaleziono wyzwania o ID: $idString")
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
                DomainError.NotFound("Nie znaleziono wyzwania o ID: $idString do usunięcia")
            }
        }

    private fun validateRequest(request: UpsertChallengeRequest): Either<DomainError.Validation, Unit> =
        either {
            val errors =
                buildList {
                    if (request.title.isBlank()) {
                        add(FieldError("title", FieldErrorCode.Blank, "Tytuł nie może być pusty"))
                    } else if (request.title.trim().length !in 3..120) {
                        add(FieldError("title", FieldErrorCode.Range(3, 120), "Tytuł musi mieć od 3 do 120 znaków"))
                    }

                    if (request.description.isBlank()) {
                        add(FieldError("description", FieldErrorCode.Blank, "Opis nie może być pusty"))
                    }

                    request.municipalities.forEachIndexed { index, mun ->
                        if (mun.isBlank()) {
                            add(
                                FieldError(
                                    "municipalities[$index]",
                                    FieldErrorCode.Blank,
                                    "Nazwa gminy nie może być pusta",
                                ),
                            )
                        }
                    }
                }

            ensure(errors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji danych wyzwania",
                    details = errors,
                )
            }
        }
}
