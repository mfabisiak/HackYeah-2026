package io.github.mfabisiak.hubmi.tester

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.allOf
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import io.github.mfabisiak.hubmi.common.mongo.requireDocument
import io.github.mfabisiak.hubmi.innovations.InnovationId
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import com.mongodb.client.model.Updates as MongoUpdates

const val FEEDBACKS_COLLECTION = "feedbacks"

val MongoDatabase.feedbacks: MongoCollection<FeedbackItem>
    get() = getCollection<FeedbackItem>(FEEDBACKS_COLLECTION)

class FeedbackRepository(
    database: MongoDatabase,
) {
    private val collection = database.feedbacks

    /** Atomically creates the user's rating of the innovation or overwrites the previous one. */
    suspend fun upsert(
        innovationId: InnovationId,
        userId: UserId,
        draft: FeedbackDraft,
        now: String,
    ): Either<RepositoryError, FeedbackItem> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(
                    Filters.eq(FeedbackItem::innovationId, innovationId.value),
                    Filters.eq(FeedbackItem::userId, userId.value),
                ),
                MongoUpdates.combine(
                    Updates.set(FeedbackItem::rating, draft.rating.value),
                    Updates.set(FeedbackItem::comment, draft.comment?.value),
                    Updates.set(FeedbackItem::suggestion, draft.suggestion?.value),
                    Updates.set(FeedbackItem::updatedAt, now),
                    Updates.setOnInsert(FeedbackItem::createdAt, now),
                ),
                FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
            )
        }.requireDocument()

    suspend fun findMine(
        innovationId: InnovationId,
        userId: UserId,
    ): Either<RepositoryError, FeedbackItem?> =
        mongoCatch {
            collection
                .find(
                    Filters.and(
                        Filters.eq(FeedbackItem::innovationId, innovationId.value),
                        Filters.eq(FeedbackItem::userId, userId.value),
                    ),
                ).firstOrNull()
        }

    /** Newest first, optionally narrowed to one innovation. */
    suspend fun findPage(
        innovationId: InnovationId?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<FeedbackItem>> =
        mongoCatch {
            val filter: Bson =
                allOf(listOfNotNull(innovationId?.let { Filters.eq(FeedbackItem::innovationId, it.value) }))
            Page(
                items =
                    collection
                        .find(filter)
                        .sort(Sorts.descending(FeedbackItem::id))
                        .skip(pageRequest.skip)
                        .limit(pageRequest.limit)
                        .toList(),
                page = pageRequest.page,
                size = pageRequest.size,
                total = collection.countDocuments(filter).toInt(),
            )
        }

    /** Derived from the source documents on every call, so a lost or interleaved write heals on the next rating. */
    suspend fun ratingTotals(innovationId: InnovationId): Either<RepositoryError, RatingTotals> =
        mongoCatch {
            collection
                .find(Filters.eq(FeedbackItem::innovationId, innovationId.value))
                .toList()
                .map(FeedbackItem::rating)
                .let { ratings -> RatingTotals(sum = ratings.sum(), count = ratings.size) }
        }
}
