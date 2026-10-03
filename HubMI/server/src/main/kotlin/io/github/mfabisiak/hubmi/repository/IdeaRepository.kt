package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.models.IdeaItem
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.time.Instant

class IdeaRepository(
    database: MongoDatabase,
) {
    private val collection = database.ideas

    suspend fun create(item: IdeaItem): Either<RepositoryError, IdeaItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findById(id: ObjectId): Either<RepositoryError, IdeaItem?> =
        mongoCatch {
            collection.find(Filters.eq(IdeaItem::id, id)).toList().firstOrNull()
        }

    suspend fun findByAuthorId(
        authorId: String,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<IdeaItem>> =
        mongoCatch {
            val filter = Filters.eq(IdeaItem::authorId, authorId)
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(IdeaItem::createdAt))
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

    suspend fun findAll(
        status: IdeaStatus?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<IdeaItem>> =
        mongoCatch {
            val filter = status?.let { Filters.eq(IdeaItem::status, it) } ?: org.bson.BsonDocument()
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(IdeaItem::createdAt))
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

    suspend fun atomicUpdateStatus(
        id: ObjectId,
        allowedPreviousStatuses: Set<IdeaStatus>,
        newStatus: IdeaStatus,
        comment: String?,
        updatedAt: Instant,
    ): Either<RepositoryError, IdeaItem?> =
        mongoCatch {
            val filter =
                Filters.and(
                    Filters.eq(IdeaItem::id, id),
                    Filters.`in`(IdeaItem::status, allowedPreviousStatuses),
                )
            val update =
                Updates.combine(
                    Updates.set(IdeaItem::status, newStatus),
                    Updates.set(IdeaItem::adminComment, comment),
                    Updates.set(IdeaItem::updatedAt, updatedAt),
                )
            val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            collection.findOneAndUpdate(filter, update, options)
        }
}
