package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import org.bson.types.ObjectId

class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    suspend fun list(
        callerId: String,
        callerRoles: Set<Role>,
        unreadOnly: Boolean,
        page: Int,
        size: Int,
    ): Either<DomainError, Page<NotificationDto>> =
        either {
            val pageRequest = validatePageRequest(page, size).bind()
            val notificationsPage =
                notificationRepository
                    .findByUser(callerId, callerRoles, unreadOnly, pageRequest)
                    .mapLeft { it.toDomainError() }
                    .bind()

            Page(
                items = notificationsPage.items.map { it.toDto(callerId) },
                page = notificationsPage.page,
                size = notificationsPage.size,
                total = notificationsPage.total,
            )
        }

    suspend fun markRead(
        idString: String,
        callerId: String,
        callerRoles: Set<Role>,
    ): Either<DomainError, Unit> =
        either {
            ensure(ObjectId.isValid(idString)) {
                DomainError.NotFound("Nie znaleziono powiadomienia o id: $idString")
            }
            val id = ObjectId(idString)
            val updated =
                notificationRepository
                    .markRead(id, callerId, callerRoles)
                    .mapLeft { it.toDomainError() }
                    .bind()

            ensure(updated) {
                DomainError.NotFound("Nie znaleziono powiadomienia o id: $idString")
            }
        }
}
