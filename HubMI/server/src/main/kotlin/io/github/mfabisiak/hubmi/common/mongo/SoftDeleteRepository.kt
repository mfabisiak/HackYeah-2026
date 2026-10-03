package io.github.mfabisiak.hubmi.common.mongo

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.common.RepositoryError
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import kotlin.reflect.KProperty
import com.mongodb.client.model.Updates as MongoUpdates

/**
 * Shared CRUD of the collections whose documents are archived instead of deleted: every read and write sees only
 * documents with `archived == false`. Subclasses add their own filters and expose typed ids.
 */
abstract class SoftDeleteRepository<T : Any>(
    private val collection: MongoCollection<T>,
    private val idField: KProperty<ObjectId>,
    private val archivedField: KProperty<Boolean>,
    private val updatedAtField: KProperty<String>,
) {
    suspend fun create(item: T): Either<RepositoryError, T> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    protected suspend fun findPage(
        filters: List<Bson>,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<T>> =
        mongoCatch {
            val filter = active(*filters.toTypedArray())
            Page(
                items =
                    collection
                        .find(filter)
                        .sort(Sorts.descending(idField))
                        .skip(pageRequest.skip)
                        .limit(pageRequest.limit)
                        .toList(),
                page = pageRequest.page,
                size = pageRequest.size,
                total = collection.countDocuments(filter).toInt(),
            )
        }

    protected suspend fun findActive(id: ObjectId): Either<RepositoryError, T?> =
        mongoCatch { collection.find(active(Filters.eq(idField, id))).firstOrNull() }

    protected suspend fun updateActive(
        id: ObjectId,
        now: String,
        vararg changes: Bson,
    ): Either<RepositoryError, T?> =
        mongoCatch {
            collection.findOneAndUpdate(
                active(Filters.eq(idField, id)),
                MongoUpdates.combine(*changes, Updates.set(updatedAtField, now)),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
        }

    suspend fun findAllActive(): Either<RepositoryError, List<T>> = mongoCatch { collection.find(active()).toList() }

    /** Changes derived data without touching `updatedAt`; returns whether an active document matched. */
    protected suspend fun patchActive(
        id: ObjectId,
        vararg changes: Bson,
    ): Either<RepositoryError, Boolean> =
        mongoCatch {
            collection.updateOne(active(Filters.eq(idField, id)), MongoUpdates.combine(*changes)).matchedCount > 0
        }

    /** Looks documents up regardless of the archived flag, e.g. to label feedback left on a since removed innovation. */
    protected suspend fun findAnyByIds(ids: Collection<ObjectId>): Either<RepositoryError, List<T>> =
        mongoCatch { collection.find(Filters.`in`(idField, ids)).toList() }

    protected suspend fun archive(
        id: ObjectId,
        now: String,
    ): Either<RepositoryError, Boolean> =
        mongoCatch {
            collection
                .updateOne(
                    active(Filters.eq(idField, id)),
                    MongoUpdates.combine(Updates.set(archivedField, true), Updates.set(updatedAtField, now)),
                ).matchedCount > 0
        }

    private fun active(vararg filters: Bson): Bson = Filters.and(Filters.eq(archivedField, false), *filters)
}
