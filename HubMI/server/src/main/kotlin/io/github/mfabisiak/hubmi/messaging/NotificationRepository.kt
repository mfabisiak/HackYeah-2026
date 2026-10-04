package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

open class NotificationRepository(
    database: MongoDatabase,
) {
    private val collection = database.notifications

    open suspend fun create(item: NotificationItem): Either<RepositoryError, NotificationItem> =
        mongoCatch {
            if (item.eventId != null) {
                val existing =
                    collection
                        .find(
                            Filters.and(
                                Filters.eq(NotificationItem::eventId, item.eventId),
                                if (item.recipientId != null) {
                                    Filters.eq(NotificationItem::recipientId, item.recipientId)
                                } else {
                                    Filters.eq(NotificationItem::targetRole, item.targetRole)
                                },
                            ),
                        ).toList()
                        .firstOrNull()
                if (existing != null) {
                    return@mongoCatch existing
                }
            }
            collection.insertOne(item)
            item
        }

    suspend fun findByUser(
        userId: String,
        roles: Set<Role>,
        unreadOnly: Boolean,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<NotificationItem>> =
        mongoCatch {
            val filter =
                if (unreadOnly) {
                    Filters.or(
                        Filters.and(
                            Filters.eq(NotificationItem::recipientId, userId),
                            Filters.eq(NotificationItem::read, false),
                        ),
                        Filters.and(
                            Filters.`in`(NotificationItem::targetRole, roles),
                            Filters.nin(NotificationItem::readByUserIds, listOf(userId)),
                        ),
                    )
                } else {
                    Filters.or(
                        Filters.eq(NotificationItem::recipientId, userId),
                        Filters.`in`(NotificationItem::targetRole, roles),
                    )
                }

            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(NotificationItem::createdAt))
                    .skip(pageRequest.skip)
                    .limit(pageRequest.limit)
                    .toList()

            Page(
                items = items,
                page = pageRequest.page,
                size = pageRequest.size,
                total = total,
            )
        }

    suspend fun markRead(
        id: ObjectId,
        userId: String,
        roles: Set<Role>,
    ): Either<RepositoryError, Boolean> =
        mongoCatch {
            val item =
                collection.find(Filters.eq(NotificationItem::id, id)).toList().firstOrNull()
                    ?: return@mongoCatch false

            if (item.recipientId == userId) {
                collection.updateOne(
                    Filters.eq(NotificationItem::id, id),
                    Updates.set(NotificationItem::read, true),
                )
                true
            } else if (item.targetRole != null && item.targetRole in roles) {
                collection.updateOne(
                    Filters.eq(NotificationItem::id, id),
                    Updates.addToSet(NotificationItem::readByUserIds, userId),
                )
                true
            } else {
                false
            }
        }
}
