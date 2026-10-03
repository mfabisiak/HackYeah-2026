package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Projections
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.hasElement
import io.github.mfabisiak.hubmi.common.mongo.mongoCatch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.bson.types.ObjectId

const val NEEDS_COLLECTION = "needs"

val MongoDatabase.needs: MongoCollection<NeedItem>
    get() = getCollection<NeedItem>(NEEDS_COLLECTION)

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

    suspend fun findTrendRowsSince(since: String): Either<RepositoryError, List<NeedTrendRow>> =
        mongoCatch {
            collection
                .find<NeedTrendRow>(Filters.gte(NeedItem::createdAt, since))
                .projection(
                    Projections.fields(
                        Projections.include(NeedItem::areas, NeedItem::municipality, NeedItem::createdAt),
                        Projections.excludeId(),
                    ),
                ).toList()
        }

    suspend fun findUnmatchedSince(since: String): Either<RepositoryError, List<NeedItem>> =
        mongoCatch { collection.find(unmatchedSince(since)).toList() }

    suspend fun countUnmatchedSince(since: String): Either<RepositoryError, Int> =
        mongoCatch { collection.countDocuments(unmatchedSince(since)).toInt() }

    private fun unmatchedSince(since: String): Bson =
        Filters.and(
            Filters.gte(NeedItem::createdAt, since),
            Filters.or(
                Filters.eq(NeedItem::noGoodMatch, true),
                Filters.size(NeedItem::matchedInnovationIds, 0),
            ),
        )
}
