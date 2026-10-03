package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.Instant
import com.mongodb.client.model.Filters as JavaFilters
import com.mongodb.client.model.Updates as JavaUpdates

class ThreadRepository(
    database: MongoDatabase,
) {
    private val collection = database.threads

    suspend fun create(item: ThreadItem): Either<RepositoryError, ThreadItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findById(id: ObjectId): Either<RepositoryError, ThreadItem?> =
        mongoCatch {
            collection.find(Filters.eq(ThreadItem::id, id)).toList().firstOrNull()
        }

    suspend fun findByParticipant(
        userId: String,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<ThreadItem>> =
        findWithFilter(
            filter =
                Filters.or(
                    Filters.eq(ThreadItem::ownerId, userId),
                    JavaFilters.eq("participantIds", userId),
                ),
            pageRequest = pageRequest,
        )

    suspend fun findAll(pageRequest: PageRequest): Either<RepositoryError, Page<ThreadItem>> =
        findWithFilter(
            filter = BsonDocument(),
            pageRequest = pageRequest,
        )

    private suspend fun findWithFilter(
        filter: Bson,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<ThreadItem>> =
        mongoCatch {
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(ThreadItem::lastMessageAt))
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

    suspend fun updateLastMessage(
        threadId: ObjectId,
        lastMessageAt: Instant,
        lastMessageBy: String,
        participantId: String,
        lastMessageRole: ParticipantRole? = null,
    ): Either<RepositoryError, Unit> =
        mongoCatch {
            val updates =
                listOfNotNull(
                    Updates.set(ThreadItem::lastMessageAt, lastMessageAt),
                    Updates.set(ThreadItem::lastMessageBy, lastMessageBy),
                    lastMessageRole?.let { Updates.set(ThreadItem::lastMessageRole, it) },
                    Updates.addToSet(ThreadItem::participantIds, participantId),
                    Updates.set(ThreadItem::updatedAt, lastMessageAt),
                    JavaUpdates.set("lastReadAt.$participantId", lastMessageAt),
                )
            collection.updateOne(
                Filters.eq(ThreadItem::id, threadId),
                Updates.combine(updates),
            )
        }.map { }

    suspend fun markRead(
        threadId: ObjectId,
        userId: String,
        at: Instant,
    ): Either<RepositoryError, Unit> =
        mongoCatch {
            collection.updateOne(
                Filters.eq(ThreadItem::id, threadId),
                JavaUpdates.set("lastReadAt.$userId", at),
            )
        }.map { }

    suspend fun countPendingAdminReply(): Either<RepositoryError, Int> =
        mongoCatch {
            collection
                .countDocuments(
                    Filters.ne(ThreadItem::lastMessageRole, ParticipantRole.ADMIN),
                ).toInt()
        }
}
