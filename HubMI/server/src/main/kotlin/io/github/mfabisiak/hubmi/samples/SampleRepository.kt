package io.github.mfabisiak.hubmi.samples

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

const val SAMPLES_COLLECTION = "samples"

val MongoDatabase.samples: MongoCollection<SampleItem>
    get() = getCollection<SampleItem>(SAMPLES_COLLECTION)

class SampleRepository(
    database: MongoDatabase,
) {
    private val collection = database.samples

    suspend fun findAll(pageRequest: PageRequest): Either<RepositoryError, Page<SampleItem>> =
        mongoCatch {
            val total = collection.countDocuments().toInt()
            val items =
                collection
                    .find()
                    .sort(Sorts.descending(SampleItem::id))
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

    suspend fun findById(id: ObjectId): Either<RepositoryError, SampleItem?> =
        mongoCatch {
            collection.find(Filters.eq(SampleItem::id, id)).toList().firstOrNull()
        }

    suspend fun create(item: SampleItem): Either<RepositoryError, SampleItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun deleteById(id: ObjectId): Either<RepositoryError, Boolean> =
        mongoCatch {
            val result = collection.deleteOne(Filters.eq(SampleItem::id, id))
            result.deletedCount > 0
        }
}
