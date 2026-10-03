package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.BsonDocument
import org.bson.types.ObjectId
import java.time.Clock
import com.mongodb.client.model.Filters as JFilters

const val GRANT_CALLS_COLLECTION = "grant_calls"

val MongoDatabase.grantCalls: MongoCollection<GrantCallItem>
    get() = getCollection<GrantCallItem>(GRANT_CALLS_COLLECTION)

class GrantCallRepository(
    database: MongoDatabase,
) {
    private val collection = database.grantCalls

    suspend fun create(item: GrantCallItem): Either<RepositoryError, GrantCallItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findById(id: ObjectId): Either<RepositoryError, GrantCallItem?> =
        mongoCatch {
            collection.find(Filters.eq(GrantCallItem::id, id)).toList().firstOrNull()
        }

    suspend fun findAll(): Either<RepositoryError, List<GrantCallItem>> =
        mongoCatch {
            collection
                .find()
                .sort(Sorts.descending(GrantCallItem::opensAt))
                .toList()
        }

    suspend fun findWithStatus(
        status: CallStatus?,
        clock: Clock,
    ): Either<RepositoryError, List<GrantCallItem>> =
        mongoCatch {
            val now = clock.instant()
            val filter =
                when (status) {
                    CallStatus.OPEN -> {
                        Filters.and(
                            Filters.lte(GrantCallItem::opensAt, now),
                            Filters.gte(GrantCallItem::closesAt, now),
                        )
                    }

                    CallStatus.UPCOMING -> {
                        Filters.gt(GrantCallItem::opensAt, now)
                    }

                    CallStatus.CLOSED -> {
                        Filters.lt(GrantCallItem::closesAt, now)
                    }

                    null -> {
                        BsonDocument()
                    }
                }
            collection
                .find(filter)
                .sort(Sorts.descending(GrantCallItem::opensAt))
                .toList()
        }

    suspend fun tryIncrementApplicantSubmission(
        callId: ObjectId,
        applicantId: String,
        maxAllowed: Int = 2,
    ): Either<RepositoryError, Boolean> =
        mongoCatch {
            val filter =
                Filters.and(
                    Filters.eq(GrantCallItem::id, callId),
                    Filters.or(
                        JFilters.not(JFilters.exists("submittedCounts.$applicantId")),
                        JFilters.lt("submittedCounts.$applicantId", maxAllowed),
                    ),
                )
            val update = Updates.inc("submittedCounts.$applicantId", 1)
            val result = collection.findOneAndUpdate(filter, update)
            result != null
        }

    suspend fun update(item: GrantCallItem): Either<RepositoryError, GrantCallItem?> =
        mongoCatch {
            val filter = Filters.eq(GrantCallItem::id, item.id)
            val options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
            collection.findOneAndReplace(filter, item, options)
        }

    suspend fun deleteById(id: ObjectId): Either<RepositoryError, Boolean> =
        mongoCatch {
            val result = collection.deleteOne(Filters.eq(GrantCallItem::id, id))
            result.deletedCount > 0
        }
}
