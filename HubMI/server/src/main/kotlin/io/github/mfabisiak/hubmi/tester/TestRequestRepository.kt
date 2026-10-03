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
import io.github.mfabisiak.hubmi.api.TestRequestStatus
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

const val TEST_REQUESTS_COLLECTION = "testRequests"

val MongoDatabase.testRequests: MongoCollection<TestRequestItem>
    get() = getCollection<TestRequestItem>(TEST_REQUESTS_COLLECTION)

class TestRequestRepository(
    database: MongoDatabase,
) {
    private val collection = database.testRequests

    /**
     * Atomically creates the user's request or replaces the note of the one still `NEW`. A request that has already
     * been decided no longer matches the filter, the upsert then collides with the unique index and surfaces as
     * [RepositoryError.Conflict].
     */
    suspend fun upsert(
        innovationId: InnovationId,
        userId: UserId,
        draft: TestRequestDraft,
        now: String,
    ): Either<RepositoryError, TestRequestItem> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(
                    Filters.eq(TestRequestItem::innovationId, innovationId.value),
                    Filters.eq(TestRequestItem::userId, userId.value),
                    Filters.eq(TestRequestItem::status, TestRequestStatus.NEW),
                ),
                MongoUpdates.combine(
                    Updates.set(TestRequestItem::note, draft.note?.value),
                    Updates.set(TestRequestItem::updatedAt, now),
                    Updates.setOnInsert(TestRequestItem::createdAt, now),
                ),
                FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
            )
        }.requireDocument()

    suspend fun findMine(
        innovationId: InnovationId,
        userId: UserId,
    ): Either<RepositoryError, TestRequestItem?> =
        mongoCatch {
            collection
                .find(
                    Filters.and(
                        Filters.eq(TestRequestItem::innovationId, innovationId.value),
                        Filters.eq(TestRequestItem::userId, userId.value),
                    ),
                ).firstOrNull()
        }

    suspend fun findById(id: TestRequestId): Either<RepositoryError, TestRequestItem?> =
        mongoCatch { collection.find(Filters.eq(TestRequestItem::id, id.value)).firstOrNull() }

    /** Newest first, optionally narrowed to one innovation and/or status. */
    suspend fun findPage(
        innovationId: InnovationId?,
        status: TestRequestStatus?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<TestRequestItem>> =
        mongoCatch {
            val filter: Bson =
                allOf(
                    listOfNotNull(
                        innovationId?.let { Filters.eq(TestRequestItem::innovationId, it.value) },
                        status?.let { Filters.eq(TestRequestItem::status, it) },
                    ),
                )
            Page(
                items =
                    collection
                        .find(filter)
                        .sort(Sorts.descending(TestRequestItem::id))
                        .skip(pageRequest.skip)
                        .limit(pageRequest.limit)
                        .toList(),
                page = pageRequest.page,
                size = pageRequest.size,
                total = collection.countDocuments(filter).toInt(),
            )
        }

    /** Moves the request from [from] to [to] only if it still is in [from]; `null` means someone else got there first. */
    suspend fun transition(
        id: TestRequestId,
        from: TestRequestStatus,
        to: TestRequestStatus,
        now: String,
    ): Either<RepositoryError, TestRequestItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(Filters.eq(TestRequestItem::id, id.value), Filters.eq(TestRequestItem::status, from)),
                MongoUpdates.combine(
                    Updates.set(TestRequestItem::status, to),
                    Updates.set(TestRequestItem::updatedAt, now),
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
        }
}
