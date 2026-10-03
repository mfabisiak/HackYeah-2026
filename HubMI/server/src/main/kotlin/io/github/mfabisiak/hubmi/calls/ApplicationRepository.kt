package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.BsonDocument
import org.bson.types.ObjectId
import java.time.Instant

const val APPLICATIONS_COLLECTION = "applications"

val MongoDatabase.applications: MongoCollection<ApplicationItem>
    get() = getCollection<ApplicationItem>(APPLICATIONS_COLLECTION)

class ApplicationRepository(
    database: MongoDatabase,
) {
    private val collection = database.applications

    suspend fun create(item: ApplicationItem): Either<RepositoryError, ApplicationItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun updateDraft(item: ApplicationItem): Either<RepositoryError, ApplicationItem?> =
        mongoCatch {
            val filter =
                Filters.and(
                    Filters.eq(ApplicationItem::id, item.id),
                    Filters.eq(ApplicationItem::status, ApplicationStatus.DRAFT),
                )
            val options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
            collection.findOneAndReplace(filter, item, options)
        }

    suspend fun submit(
        id: ObjectId,
        updatedAt: Instant,
    ): Either<RepositoryError, ApplicationItem?> =
        mongoCatch {
            val filter =
                Filters.and(
                    Filters.eq(ApplicationItem::id, id),
                    Filters.eq(ApplicationItem::status, ApplicationStatus.DRAFT),
                )
            val update =
                Updates.combine(
                    Updates.set(ApplicationItem::status, ApplicationStatus.SUBMITTED),
                    Updates.set(ApplicationItem::updatedAt, updatedAt),
                )
            val options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            collection.findOneAndUpdate(filter, update, options)
        }

    suspend fun findById(id: ObjectId): Either<RepositoryError, ApplicationItem?> =
        mongoCatch {
            collection.find(Filters.eq(ApplicationItem::id, id)).toList().firstOrNull()
        }

    suspend fun countSubmittedByCallAndApplicant(
        callId: ObjectId,
        applicantId: String,
    ): Either<RepositoryError, Long> =
        mongoCatch {
            val filter =
                Filters.and(
                    Filters.eq(ApplicationItem::callId, callId),
                    Filters.eq(ApplicationItem::applicantId, applicantId),
                    Filters.eq(ApplicationItem::status, ApplicationStatus.SUBMITTED),
                )
            collection.countDocuments(filter)
        }

    suspend fun findByApplicantId(
        applicantId: String,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<ApplicationItem>> =
        mongoCatch {
            val filter = Filters.eq(ApplicationItem::applicantId, applicantId)
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(ApplicationItem::createdAt))
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
        callId: ObjectId?,
        status: ApplicationStatus?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<ApplicationItem>> =
        mongoCatch {
            val filterParts =
                listOfNotNull(
                    callId?.let { Filters.eq(ApplicationItem::callId, it) },
                    status?.let { Filters.eq(ApplicationItem::status, it) },
                )
            val filter = if (filterParts.isEmpty()) BsonDocument() else Filters.and(filterParts)
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(ApplicationItem::createdAt))
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
}
