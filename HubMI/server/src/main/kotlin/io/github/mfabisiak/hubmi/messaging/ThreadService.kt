package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.ThreadDto
import io.github.mfabisiak.hubmi.api.isStaff
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import io.github.mfabisiak.hubmi.ideas.EventPublisher
import io.github.mfabisiak.hubmi.ideas.MessageReceived
import org.bson.types.ObjectId
import java.time.Clock

class ThreadService(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val eventPublisher: EventPublisher,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun createThread(
        callerId: String,
        callerName: String,
        callerRoles: Set<Role>,
        request: CreateThreadRequest,
    ): Either<DomainError, ThreadDto> =
        either {
            val draft = ThreadDraft.parseCreate(request).orValidationError("Błąd walidacji wątku").bind()
            val now = clock.instant()

            val authorRole =
                when {
                    Role.ADMIN in callerRoles -> ParticipantRole.ADMIN
                    Role.EXPERT in callerRoles -> ParticipantRole.EXPERT
                    else -> ParticipantRole.AUTHOR
                }

            val threadItem =
                ThreadItem(
                    ownerId = callerId,
                    ownerName = callerName,
                    subject = draft.subject,
                    relatedIdeaId = draft.relatedIdeaId,
                    participantIds = setOf(callerId),
                    lastMessageAt = now,
                    lastMessageBy = callerId,
                    lastMessageRole = authorRole,
                    lastReadAt = mapOf(callerId to now),
                    createdAt = now,
                    updatedAt = now,
                )

            val savedThread =
                threadRepository
                    .create(threadItem)
                    .mapLeft { it.toDomainError() }
                    .bind()

            val messageItem =
                MessageItem(
                    threadId = savedThread.id,
                    authorId = callerId,
                    authorName = callerName,
                    authorRole = authorRole,
                    text = draft.initialMessage,
                    sentAt = now,
                )

            val savedMessage =
                messageRepository
                    .create(messageItem)
                    .mapLeft { it.toDomainError() }
                    .bind()

            eventPublisher.publish(
                MessageReceived(
                    threadId = savedThread.id.toHexString(),
                    messageId = savedMessage.id.toHexString(),
                    senderId = callerId,
                    senderName = callerName,
                    senderRole = authorRole,
                    recipientIds = emptySet(),
                    threadSubject = savedThread.subject,
                    text = savedMessage.text,
                    occurredAt = now,
                ),
            )

            savedThread.toDto(callerId)
        }

    suspend fun listThreads(
        callerId: String,
        callerRoles: Set<Role>,
        page: Int,
        size: Int,
    ): Either<DomainError, Page<ThreadDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val threadsPage =
                if (callerRoles.isStaff()) {
                    threadRepository.findAll(pageRequest)
                } else {
                    threadRepository.findByParticipant(callerId, pageRequest)
                }.mapLeft { it.toDomainError() }.bind()

            Page(
                items = threadsPage.items.map { it.toDto(callerId) },
                page = threadsPage.page,
                size = threadsPage.size,
                total = threadsPage.total,
            )
        }

    suspend fun listMessages(
        threadIdString: String,
        callerId: String,
        callerRoles: Set<Role>,
    ): Either<DomainError, List<MessageDto>> =
        either {
            ensure(ObjectId.isValid(threadIdString)) {
                DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
            }
            val threadId = ObjectId(threadIdString)
            val thread =
                ensureNotNull(
                    threadRepository.findById(threadId).mapLeft { it.toDomainError() }.bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
                }

            val canAccess = thread.ownerId == callerId || callerId in thread.participantIds || callerRoles.isStaff()
            ensure(canAccess) {
                DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
            }

            threadRepository.markRead(threadId, callerId, clock.instant()).mapLeft { it.toDomainError() }.bind()

            val messages =
                messageRepository
                    .findByThreadId(threadId)
                    .mapLeft { it.toDomainError() }
                    .bind()

            messages.map { it.toDto() }
        }

    suspend fun postMessage(
        threadIdString: String,
        callerId: String,
        callerName: String,
        callerRoles: Set<Role>,
        request: PostMessageRequest,
    ): Either<DomainError, MessageDto> =
        either {
            ensure(ObjectId.isValid(threadIdString)) {
                DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
            }
            val threadId = ObjectId(threadIdString)
            val draft = ThreadDraft.parseMessage(request).orValidationError("Błąd walidacji wiadomości").bind()

            val thread =
                ensureNotNull(
                    threadRepository.findById(threadId).mapLeft { it.toDomainError() }.bind(),
                ) {
                    DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
                }

            val canAccess = thread.ownerId == callerId || callerId in thread.participantIds || callerRoles.isStaff()
            ensure(canAccess) {
                DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
            }

            val now = clock.instant()
            val authorRole =
                when {
                    Role.ADMIN in callerRoles -> ParticipantRole.ADMIN
                    Role.EXPERT in callerRoles -> ParticipantRole.EXPERT
                    else -> ParticipantRole.AUTHOR
                }

            val messageItem =
                MessageItem(
                    threadId = threadId,
                    authorId = callerId,
                    authorName = callerName,
                    authorRole = authorRole,
                    text = draft.text,
                    sentAt = now,
                )

            val savedMessage =
                messageRepository
                    .create(messageItem)
                    .mapLeft { it.toDomainError() }
                    .bind()

            threadRepository
                .updateLastMessage(threadId, now, callerId, callerId, authorRole)
                .mapLeft { it.toDomainError() }
                .bind()

            val recipientIds =
                if (callerId == thread.ownerId) {
                    thread.participantIds - callerId
                } else {
                    (thread.participantIds + thread.ownerId) - callerId
                }

            eventPublisher.publish(
                MessageReceived(
                    threadId = thread.id.toHexString(),
                    messageId = savedMessage.id.toHexString(),
                    senderId = callerId,
                    senderName = callerName,
                    senderRole = authorRole,
                    recipientIds = recipientIds,
                    threadSubject = thread.subject,
                    text = savedMessage.text,
                    occurredAt = now,
                ),
            )

            savedMessage.toDto()
        }

    /** Idempotent for the official who already handles the thread; a thread taken by somebody else is a conflict. */
    suspend fun assign(
        threadIdString: String,
        callerId: String,
        callerName: String,
    ): Either<DomainError, ThreadDto> =
        either {
            val thread = findStaffThread(threadIdString).bind()
            if (thread.assigneeId == callerId) {
                thread.toDto(callerId)
            } else {
                val claimed =
                    threadRepository
                        .claim(thread.id, callerId, callerName, clock.instant())
                        .mapLeft { it.toDomainError() }
                        .bind()
                ensureNotNull(claimed) { DomainError.Conflict("Ten wątek obsługuje już inny pracownik ROPS") }
                    .toDto(callerId)
            }
        }

    /** Only the assignee or an admin hands a thread back; a thread nobody handles stays as it is. */
    suspend fun unassign(
        threadIdString: String,
        callerId: String,
        callerRoles: Set<Role>,
    ): Either<DomainError, ThreadDto> =
        either {
            val thread = findStaffThread(threadIdString).bind()
            val assigneeId = thread.assigneeId
            if (assigneeId == null) {
                thread.toDto(callerId)
            } else {
                ensure(assigneeId == callerId || Role.ADMIN in callerRoles) {
                    DomainError.Forbidden("Wątek obsługuje inny pracownik ROPS")
                }
                val released =
                    threadRepository
                        .release(thread.id, assigneeId, clock.instant())
                        .mapLeft { it.toDomainError() }
                        .bind()
                ensureNotNull(released) { DomainError.Conflict("Przypisanie wątku właśnie się zmieniło") }
                    .toDto(callerId)
            }
        }

    private suspend fun findStaffThread(threadIdString: String): Either<DomainError, ThreadItem> =
        either {
            ensure(ObjectId.isValid(threadIdString)) {
                DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
            }
            ensureNotNull(
                threadRepository.findById(ObjectId(threadIdString)).mapLeft { it.toDomainError() }.bind(),
            ) {
                DomainError.NotFound("Nie znaleziono wątku o id: $threadIdString")
            }
        }
}
