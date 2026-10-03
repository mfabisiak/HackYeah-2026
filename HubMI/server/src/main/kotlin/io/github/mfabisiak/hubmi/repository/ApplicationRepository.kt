package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import io.github.mfabisiak.hubmi.models.ApplicationItem
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class ApplicationRepository(
    database: MongoDatabase,
) {
    private val collection = database.applications

    suspend fun create(item: ApplicationItem): Either<RepositoryError, ApplicationItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findById(id: ObjectId): Either<RepositoryError, ApplicationItem?> =
        mongoCatch {
            collection.find(Filters.eq(ApplicationItem::id, id)).toList().firstOrNull()
        }

    suspend fun findByCallId(callId: ObjectId): Either<RepositoryError, List<ApplicationItem>> =
        mongoCatch {
            collection
                .find(Filters.eq(ApplicationItem::callId, callId))
                .sort(Sorts.descending(ApplicationItem::createdAt))
                .toList()
        }

    suspend fun findByApplicantId(applicantId: String): Either<RepositoryError, List<ApplicationItem>> =
        mongoCatch {
            collection
                .find(Filters.eq(ApplicationItem::applicantId, applicantId))
                .sort(Sorts.descending(ApplicationItem::createdAt))
                .toList()
        }
}
