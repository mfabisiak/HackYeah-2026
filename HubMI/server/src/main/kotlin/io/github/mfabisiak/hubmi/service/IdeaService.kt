package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.models.IdeaItem
import io.github.mfabisiak.hubmi.models.toDto
import io.github.mfabisiak.hubmi.repository.IdeaRepository
import org.bson.types.ObjectId
import java.time.Instant

class IdeaService(
    private val repository: IdeaRepository,
    private val eventPublisher: EventPublisher,
) {
    companion object {
        val ALLOWED_TRANSITIONS: Map<IdeaStatus, Set<IdeaStatus>> =
            mapOf(
                IdeaStatus.DRAFT to setOf(IdeaStatus.SUBMITTED),
                IdeaStatus.SUBMITTED to setOf(IdeaStatus.IN_REVIEW),
                IdeaStatus.IN_REVIEW to setOf(IdeaStatus.ACCEPTED, IdeaStatus.REJECTED),
                IdeaStatus.ACCEPTED to emptySet(),
                IdeaStatus.REJECTED to emptySet(),
            )
    }

    suspend fun create(
        callerId: String,
        request: CreateIdeaRequest,
    ): Either<DomainError, IdeaDto> =
        either {
            val fieldErrors =
                buildList {
                    if (request.title.isBlank()) {
                        add(FieldError("title", FieldErrorCode.Blank, "Tytuł pomysłu nie może być pusty"))
                    }
                    if (request.essence.isBlank()) {
                        add(FieldError("essence", FieldErrorCode.Blank, "Istota pomysłu nie może być pusta"))
                    }
                    if (request.targetGroups.isEmpty()) {
                        add(
                            FieldError(
                                field = "targetGroups",
                                code = FieldErrorCode.Blank,
                                message = "Należy wybrać przynajmniej jedną grupę docelową",
                            ),
                        )
                    }
                }

            ensure(fieldErrors.isEmpty()) {
                DomainError.Validation(
                    message = "Błąd walidacji formularza pomysłu",
                    details = fieldErrors,
                )
            }

            val now = Instant.now()
            val item =
                IdeaItem(
                    authorId = callerId,
                    title = request.title.trim(),
                    essence = request.essence.trim(),
                    targetGroups = request.targetGroups,
                    stage = request.stage,
                    status = IdeaStatus.SUBMITTED,
                    createdAt = now,
                    updatedAt = now,
                )

            val created =
                repository
                    .create(item)
                    .mapLeft { it.toDomainError() }
                    .bind()

            eventPublisher.publish(
                IdeaSubmitted(
                    ideaId = created.id.toHexString(),
                    authorId = callerId,
                    title = created.title,
                    occurredAt = now,
                ),
            )

            created.toDto()
        }

    suspend fun getById(
        idString: String,
        callerId: String,
        callerRoles: Set<Role>,
    ): Either<DomainError, IdeaDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val idea =
                ensureNotNull(
                    repository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono pomysłu o id: $idString")
                }

            val canAccess = idea.authorId == callerId || Role.ADMIN in callerRoles || Role.EXPERT in callerRoles
            ensure(canAccess) {
                DomainError.Forbidden("Brak uprawnień do przeglądania tego pomysłu")
            }

            idea.toDto()
        }

    suspend fun getMine(
        callerId: String,
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<IdeaDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val result =
                repository
                    .findByAuthorId(callerId, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()

            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun adminList(
        status: IdeaStatus?,
        page: Int?,
        size: Int?,
    ): Either<DomainError, Page<IdeaDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val result =
                repository
                    .findAll(status, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()

            Page(
                items = result.items.map { it.toDto() },
                page = result.page,
                size = result.size,
                total = result.total,
            )
        }

    suspend fun adminUpdateStatus(
        idString: String,
        request: UpdateIdeaStatusRequest,
    ): Either<DomainError, IdeaDto> =
        either {
            val objectId = parseObjectId(idString).bind()

            if (request.status == IdeaStatus.REJECTED) {
                ensure(!request.comment.isNullOrBlank()) {
                    DomainError.Validation(
                        message = "Komentarz dla autora jest wymagany przy odrzuceniu pomysłu",
                        details =
                            listOf(
                                FieldError(
                                    field = "comment",
                                    code = FieldErrorCode.Blank,
                                    message = "Komentarz dla autora jest wymagany przy odrzuceniu pomysłu",
                                ),
                            ),
                    )
                }
            }

            val allowedPrevious =
                ALLOWED_TRANSITIONS.entries
                    .filter { request.status in it.value }
                    .map { it.key }
                    .toSet()

            ensure(allowedPrevious.isNotEmpty()) {
                val existing =
                    repository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind()
                ensureNotNull(existing) { DomainError.NotFound("Nie znaleziono pomysłu o id: $idString") }
                DomainError.Conflict("Niedozwolona zmiana statusu na: ${request.status}")
            }

            val now = Instant.now()
            val updated =
                repository
                    .atomicUpdateStatus(
                        id = objectId,
                        allowedPreviousStatuses = allowedPrevious,
                        newStatus = request.status,
                        comment = request.comment?.trim(),
                        updatedAt = now,
                    ).mapLeft { it.toDomainError() }
                    .bind()

            if (updated != null) {
                eventPublisher.publish(
                    IdeaStatusChanged(
                        ideaId = updated.id.toHexString(),
                        authorId = updated.authorId,
                        oldStatus = allowedPrevious.first(),
                        newStatus = updated.status,
                        adminComment = updated.adminComment,
                        occurredAt = now,
                    ),
                )
                updated.toDto()
            } else {
                val existing =
                    ensureNotNull(
                        repository
                            .findById(objectId)
                            .mapLeft { it.toDomainError() }
                            .bind(),
                    ) {
                        DomainError.NotFound("Nie znaleziono pomysłu o id: $idString")
                    }

                raise(DomainError.Conflict("Niedozwolona zmiana statusu z ${existing.status} na ${request.status}"))
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
