package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.allOf
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson

const val ADAPTATIONS_COLLECTION = "adaptations"

val MongoDatabase.adaptations: MongoCollection<AdaptationItem>
    get() = getCollection<AdaptationItem>(ADAPTATIONS_COLLECTION)

class AdaptationRepository(
    database: MongoDatabase,
) {
    private val collection = database.adaptations

    suspend fun create(item: AdaptationItem): Either<RepositoryError, AdaptationItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findById(id: AdaptationId): Either<RepositoryError, AdaptationItem?> =
        mongoCatch { collection.find(Filters.eq(AdaptationItem::id, id.value)).firstOrNull() }

    /** Newest first; [author] narrows it to one user's plans, [status] to one stage of the review. */
    suspend fun findPage(
        author: UserId?,
        status: AdaptationStatus?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<AdaptationItem>> =
        mongoCatch {
            val filter: Bson =
                allOf(
                    listOfNotNull(
                        author?.let { Filters.eq(AdaptationItem::authorId, it.value) },
                        status?.let { Filters.eq(AdaptationItem::status, it) },
                    ),
                )
            Page(
                items =
                    collection
                        .find(filter)
                        .sort(Sorts.descending(AdaptationItem::id))
                        .skip(pageRequest.skip)
                        .limit(pageRequest.limit)
                        .toList(),
                page = pageRequest.page,
                size = pageRequest.size,
                total = collection.countDocuments(filter).toInt(),
            )
        }

    /** Moves the plan from [from] to [to] only if it still is in [from]; `null` means someone else got there first. */
    suspend fun transition(
        id: AdaptationId,
        from: AdaptationStatus,
        to: AdaptationStatus,
        comment: ReviewComment?,
        now: String,
    ): Either<RepositoryError, AdaptationItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(Filters.eq(AdaptationItem::id, id.value), Filters.eq(AdaptationItem::status, from)),
                Updates.combine(
                    listOfNotNull(
                        Updates.set(AdaptationItem::status, to),
                        comment?.let { Updates.set(AdaptationItem::adminComment, it.value) },
                        Updates.set(AdaptationItem::updatedAt, now),
                    ),
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
        }
}
