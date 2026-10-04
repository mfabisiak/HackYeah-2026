package io.github.mfabisiak.hubmi.messaging

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
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
        lastMessageRole: ParticipantRole,
    ): Either<RepositoryError, Unit> =
        mongoCatch {
            collection.updateOne(
                Filters.eq(ThreadItem::id, threadId),
                Updates.combine(
                    Updates.set(ThreadItem::lastMessageAt, lastMessageAt),
                    Updates.set(ThreadItem::lastMessageBy, lastMessageBy),
                    Updates.set(ThreadItem::lastMessageRole, lastMessageRole),
                    Updates.addToSet(ThreadItem::participantIds, participantId),
                    Updates.set(ThreadItem::updatedAt, lastMessageAt),
                    JavaUpdates.set("lastReadAt.$participantId", lastMessageAt),
                ),
            )
        }.map { }

    /** Takes an unassigned thread over; `null` when the thread is missing or somebody else already handles it. */
    suspend fun claim(
        threadId: ObjectId,
        assigneeId: String,
        assigneeName: String,
        at: Instant,
    ): Either<RepositoryError, ThreadItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(Filters.eq(ThreadItem::id, threadId), Filters.eq(ThreadItem::assigneeId, null)),
                Updates.combine(
                    Updates.set(ThreadItem::assigneeId, assigneeId),
                    Updates.set(ThreadItem::assigneeName, assigneeName),
                    Updates.set(ThreadItem::updatedAt, at),
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
        }

    /** Hands the thread back; `null` when it is missing or [currentAssigneeId] no longer handles it. */
    suspend fun release(
        threadId: ObjectId,
        currentAssigneeId: String,
        at: Instant,
    ): Either<RepositoryError, ThreadItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(
                    Filters.eq(ThreadItem::id, threadId),
                    Filters.eq(ThreadItem::assigneeId, currentAssigneeId),
                ),
                Updates.combine(
                    Updates.unset(ThreadItem::assigneeId),
                    Updates.unset(ThreadItem::assigneeName),
                    Updates.set(ThreadItem::updatedAt, at),
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
        }

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

    suspend fun countPendingStaffReply(): Either<RepositoryError, Int> =
        mongoCatch {
            collection
                .countDocuments(
                    Filters.nin(ThreadItem::lastMessageRole, listOf(ParticipantRole.ADMIN, ParticipantRole.EXPERT)),
                ).toInt()
        }
}
