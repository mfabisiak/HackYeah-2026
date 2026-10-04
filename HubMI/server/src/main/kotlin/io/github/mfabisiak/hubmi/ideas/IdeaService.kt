package io.github.mfabisiak.hubmi.ideas

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
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpdateIdeaStatusRequest
import io.github.mfabisiak.hubmi.api.isStaff
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import org.bson.types.ObjectId
import java.time.Clock

data class IdeaPrefill(
    val ideaId: ObjectId,
    val title: String,
    val essence: String,
    val targetGroups: List<TargetGroup>,
)

class IdeaService(
    private val repository: IdeaRepository,
    private val eventPublisher: EventPublisher,
    private val clock: Clock = Clock.systemUTC(),
) {
    companion object {
        const val MAX_ADMIN_COMMENT_LENGTH = 1000

        val ALLOWED_TRANSITIONS: Map<IdeaStatus, Set<IdeaStatus>> =
            mapOf(
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
            val draft = IdeaDraft.parse(request).orValidationError("Błąd walidacji formularza pomysłu").bind()

            val now = clock.instant()
            val item =
                IdeaItem(
                    authorId = callerId,
                    title = draft.title,
                    essence = draft.essence,
                    targetGroups = draft.targetGroups.distinct(),
                    stage = draft.stage,
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

            val canAccess = idea.authorId == callerId || callerRoles.isStaff()
            ensure(canAccess) {
                DomainError.Forbidden("Brak uprawnień do przeglądania tego pomysłu")
            }

            idea.toDto()
        }

    suspend fun getPrefill(
        idString: String,
        callerId: String,
    ): Either<DomainError, IdeaPrefill> =
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

            ensure(idea.authorId == callerId) {
                DomainError.Forbidden("Wskazany pomysł nie należy do Ciebie")
            }

            IdeaPrefill(
                ideaId = idea.id,
                title = idea.title,
                essence = idea.essence,
                targetGroups = idea.targetGroups,
            )
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

    suspend fun listForAdmin(
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

    suspend fun updateStatus(
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
            val comment = request.comment
            if (comment != null) {
                ensure(comment.trim().length <= MAX_ADMIN_COMMENT_LENGTH) {
                    DomainError.Validation(
                        message = "Komentarz dla autora nie może przekraczać $MAX_ADMIN_COMMENT_LENGTH znaków",
                        details =
                            listOf(
                                FieldError(
                                    field = "comment",
                                    code = FieldErrorCode.InvalidFormat,
                                    message =
                                        "Komentarz dla autora nie może przekraczać $MAX_ADMIN_COMMENT_LENGTH znaków",
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

            val now = clock.instant()
            val before =
                repository
                    .atomicUpdateStatus(
                        id = objectId,
                        allowedPreviousStatuses = allowedPrevious,
                        newStatus = request.status,
                        comment = request.comment?.trim(),
                        updatedAt = now,
                    ).mapLeft { it.toDomainError() }
                    .bind()

            if (before != null) {
                val oldStatus = before.status
                eventPublisher.publish(
                    IdeaStatusChanged(
                        ideaId = before.id.toHexString(),
                        authorId = before.authorId,
                        oldStatus = oldStatus,
                        newStatus = request.status,
                        occurredAt = now,
                    ),
                )
                val updatedDto =
                    before.toDto().copy(
                        status = request.status,
                        adminComment = request.comment?.trim() ?: before.adminComment,
                    )
                updatedDto
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
