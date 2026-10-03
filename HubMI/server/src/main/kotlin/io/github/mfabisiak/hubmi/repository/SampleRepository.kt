package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.models.SampleItem
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class SampleRepository(
    database: MongoDatabase,
) {
    private val collection = database.getCollection<SampleItem>("samples")

    suspend fun findAll(pageRequest: PageRequest): Either<RepositoryError, Page<SampleItem>> =
        mongoCatch {
            val total = collection.countDocuments().toInt()
            val items =
                collection
                    .find()
                    .sort(Sorts.descending("_id"))
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
            collection.find(Filters.eq("_id", id)).toList().firstOrNull()
        }

    suspend fun findBySlug(slug: String): Either<RepositoryError, SampleItem?> =
        mongoCatch {
            collection.find(Filters.eq("slug", slug)).toList().firstOrNull()
        }

    suspend fun create(item: SampleItem): Either<RepositoryError, SampleItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun deleteById(id: ObjectId): Either<RepositoryError, Boolean> =
        mongoCatch {
            val result = collection.deleteOne(Filters.eq("_id", id))
            result.deletedCount > 0
        }
}
