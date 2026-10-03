package io.github.mfabisiak.hubmi.samples

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.CreateSampleRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SampleDto
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.mongo.matches
import io.github.mfabisiak.hubmi.common.parseObjectId
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.common.validatePageRequest
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
            val fieldErrors =
                buildList {
                    if (request.slug.isBlank()) {
                        add(FieldError("slug", FieldErrorCode.Blank, "Pole 'slug' nie może być puste"))
                    } else if (!request.slug.matches(Regex("^[a-z0-9-]+$"))) {
                        add(
                            FieldError(
                                field = "slug",
                                code = FieldErrorCode.InvalidFormat,
                                message = "Pole 'slug' może zawierać tylko małe litery, cyfry i myślniki",
                            ),
                        )
                    }
                    if (request.name.isBlank()) {
                        add(FieldError("name", FieldErrorCode.Blank, "Pole 'name' nie może być puste"))
                    }
                    if (request.description.isBlank()) {
                        add(FieldError("description", FieldErrorCode.Blank, "Pole 'description' nie może być puste"))
                    }
                }

            ensure(fieldErrors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji danych wejściowych",
                    details = fieldErrors,
                )
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
