package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.models.MaterialItem
import io.github.mfabisiak.hubmi.models.toDto
import io.github.mfabisiak.hubmi.repository.MaterialRepository
import java.time.Instant

class MaterialService(
    private val repository: MaterialRepository,
) {
    suspend fun list(
        q: String?,
        area: SocialArea?,
        type: MaterialType?,
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<MaterialDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val result =
                repository
                    .findAll(q, area, type, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(idString: String): Either<DomainError, MaterialDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val item =
                ensureNotNull(
                    repository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono materiału o ID: $idString")
                }
            item.toDto()
        }

    suspend fun create(request: UpsertMaterialRequest): Either<DomainError, MaterialDto> =
        either {
            validateRequest(request).bind()
            val now = Instant.now().toString()
            val item =
                MaterialItem(
                    title = request.title.trim(),
                    description = request.description.trim(),
                    type = request.type,
                    url = request.url.trim(),
                    areas = request.areas,
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
        request: UpsertMaterialRequest,
    ): Either<DomainError, MaterialDto> =
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
                    DomainError.NotFound("Nie znaleziono materiału o ID: $idString")
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
                DomainError.NotFound("Nie znaleziono materiału o ID: $idString do usunięcia")
            }
        }

    private fun validateRequest(request: UpsertMaterialRequest): Either<DomainError.Validation, Unit> =
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

                    val trimmedUrl = request.url.trim()
                    if (trimmedUrl.isBlank()) {
                        add(FieldError("url", FieldErrorCode.Blank, "Adres URL nie może być pusty"))
                    } else if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                        add(FieldError("url", FieldErrorCode.InvalidFormat, "Niepoprawny format adresu URL"))
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
                }

            ensure(errors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji danych materiału",
                    details = errors,
                )
            }
        }
}
