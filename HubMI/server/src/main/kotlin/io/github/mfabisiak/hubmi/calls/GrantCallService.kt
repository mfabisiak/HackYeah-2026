package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.ideas.CallChanged
import io.github.mfabisiak.hubmi.ideas.CallPublished
import io.github.mfabisiak.hubmi.ideas.EventPublisher
import org.bson.types.ObjectId
import java.time.Clock

class GrantCallService(
    private val callRepository: GrantCallRepository,
    private val applicationRepository: ApplicationRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val eventPublisher: EventPublisher? = null,
) {
    suspend fun list(status: CallStatus?): Either<DomainError, List<GrantCallDto>> =
        either {
            val calls =
                callRepository
                    .findWithStatus(status, clock)
                    .mapLeft { it.toDomainError() }
                    .bind()
            calls.map { it.toDto(clock) }
        }

    suspend fun active(): Either<DomainError, List<GrantCallDto>> =
        either {
            val calls =
                callRepository
                    .findWithStatus(CallStatus.OPEN, clock)
                    .mapLeft { it.toDomainError() }
                    .bind()
            calls.map { it.toDto(clock) }
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
            val draft = CallDraft.parse(request).orValidationError("Błąd walidacji formularza naboru").bind()

            val now = clock.instant()
            val item =
                GrantCallItem(
                    title = draft.title,
                    description = draft.description,
                    opensAt = draft.opensAt,
                    closesAt = draft.closesAt,
                    fields = draft.fields,
                    createdAt = now,
                    updatedAt = now,
                )

            val created =
                callRepository
                    .create(item)
                    .mapLeft { it.toDomainError() }
                    .bind()

            eventPublisher?.publish(
                CallPublished(
                    callId = created.id.toHexString(),
                    callTitle = created.title,
                    occurredAt = now,
                ),
            )

            created.toDto(clock)
        }

    suspend fun update(
        idString: String,
        request: UpsertCallRequest,
    ): Either<DomainError, GrantCallDto> =
        either {
            val objectId = parseObjectId(idString).bind()
            val draft = CallDraft.parse(request).orValidationError("Błąd walidacji formularza naboru").bind()

            val existing =
                ensureNotNull(
                    callRepository
                        .findById(objectId)
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono naboru o id: $idString")
                }

            val hasApplications =
                applicationRepository
                    .existsByCallId(objectId)
                    .mapLeft { it.toDomainError() }
                    .bind()

            if (hasApplications && (existing.opensAt != draft.opensAt || existing.closesAt != draft.closesAt)) {
                raise(DomainError.Conflict("Nie można zmienić terminów naboru, do którego złożono już wnioski"))
            }

            val updatedItem =
                existing.copy(
                    title = draft.title,
                    description = draft.description,
                    opensAt = draft.opensAt,
                    closesAt = draft.closesAt,
                    fields = draft.fields,
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

            eventPublisher?.publish(
                CallChanged(
                    callId = updated.id.toHexString(),
                    callTitle = updated.title,
                    occurredAt = updatedItem.updatedAt,
                ),
            )

            updated.toDto(clock)
        }

    suspend fun delete(idString: String): Either<DomainError, Unit> =
        either {
            val objectId = parseObjectId(idString).bind()

            ensureNotNull(
                callRepository
                    .findById(objectId)
                    .mapLeft { it.toDomainError() }
                    .bind(),
            ) {
                DomainError.NotFound("Nie znaleziono naboru o id: $idString")
            }

            val hasApplications =
                applicationRepository
                    .existsByCallId(objectId)
                    .mapLeft { it.toDomainError() }
                    .bind()

            ensure(!hasApplications) {
                DomainError.Conflict("Nie można usunąć naboru, do którego złożono wnioski")
            }

            val deleted =
                callRepository
                    .deleteById(objectId)
                    .mapLeft { it.toDomainError() }
                    .bind()

            ensure(deleted) {
                DomainError.NotFound("Nie znaleziono naboru o id: $idString")
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
