package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.domain.NeedId
import io.github.mfabisiak.hubmi.models.NeedItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class NeedRepository(
    database: MongoDatabase,
) {
    private val collection = database.needs

    suspend fun create(item: NeedItem): Either<RepositoryError, NeedItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun findById(id: NeedId): Either<RepositoryError, NeedItem?> =
        mongoCatch { collection.find(Filters.eq(NeedItem::id, id.value)).firstOrNull() }

    /** Overwrites the answer to "did it help?"; returns whether the need exists. */
    suspend fun setHelpful(
        id: NeedId,
        helpful: Boolean,
    ): Either<RepositoryError, Boolean> =
        mongoCatch {
            collection
                .updateOne(Filters.eq(NeedItem::id, id.value), Updates.set(NeedItem::helpful, helpful))
                .matchedCount > 0
        }

    /** Newest needs whose matches included [innovationId] and for which matching found something. */
    suspend fun findSimilar(
        innovationId: ObjectId,
        limit: Int,
    ): Either<RepositoryError, List<NeedItem>> =
        mongoCatch {
            collection
                .find(
                    Filters.and(
                        NeedItem::matchedInnovationIds.hasElement(innovationId),
                        Filters.eq(NeedItem::noGoodMatch, false),
                    ),
                ).sort(Sorts.descending(NeedItem::id))
                .limit(limit)
                .toList()
        }
}
