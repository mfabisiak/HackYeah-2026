package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.CreateSampleRequest
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SampleDto
import io.github.mfabisiak.hubmi.models.SampleItem
import io.github.mfabisiak.hubmi.models.toDto
import io.github.mfabisiak.hubmi.repository.SampleRepository
import org.bson.types.ObjectId

class SampleService(
    private val repository: SampleRepository,
) {
    suspend fun list(
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<SampleDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val result =
                repository
                    .findAll(pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun getById(idString: String): Either<DomainError, SampleDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val item =
                ensureNotNull(
                    repository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono elementu o id: $idString")
                }
            item.toDto()
        }

    suspend fun create(request: CreateSampleRequest): Either<DomainError, SampleDto> =
        either {
            ensure(request.slug.isNotBlank()) {
                DomainError.ValidationFailed("Pole 'slug' nie może być puste")
            }
            ensure(request.slug.matches(Regex("^[a-z0-9-]+$"))) {
                DomainError.ValidationFailed("Pole 'slug' może zawierać tylko małe litery, cyfry i myślniki")
            }
            ensure(request.name.isNotBlank()) {
                DomainError.ValidationFailed("Pole 'name' nie może być puste")
            }
            ensure(request.description.isNotBlank()) {
                DomainError.ValidationFailed("Pole 'description' nie może być puste")
            }

            val item =
                SampleItem(
                    slug = request.slug.trim(),
                    name = request.name.trim(),
                    description = request.description.trim(),
                )
            repository
                .create(item)
                .mapLeft { it.toDomainError() }
                .bind()
                .toDto()
        }

    suspend fun delete(idString: String): Either<DomainError, Unit> =
        either {
            val objectId = parseObjectId(idString).bind()
            val deleted =
                repository
                    .deleteById(objectId)
                    .mapLeft { it.toDomainError() }
                    .bind()
            ensure(deleted) {
                DomainError.NotFound("Nie znaleziono elementu o id: $idString do usunięcia")
            }
        }

    private fun parseObjectId(idString: String): Either<DomainError.ValidationFailed, ObjectId> =
        either {
            ensure(ObjectId.isValid(idString)) {
                DomainError.ValidationFailed("Nieprawidłowy format ID: $idString")
            }
            ObjectId(idString)
        }
}
