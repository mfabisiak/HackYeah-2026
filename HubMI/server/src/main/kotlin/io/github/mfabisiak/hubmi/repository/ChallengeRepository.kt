package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.models.ChallengeItem
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

class ChallengeRepository(
    database: MongoDatabase,
) {
    private val collection = database.challenges

    suspend fun findAll(
        area: SocialArea?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<ChallengeItem>> =
        mongoCatch {
            val filters =
                buildList {
                    add(Filters.eq(ChallengeItem::archived, false))
                    if (area != null) {
                        add(Filters.eq(ChallengeItem::area, area))
                    }
                }
            val filter = Filters.and(filters)
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(ChallengeItem::id))
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

    suspend fun findById(id: ObjectId): Either<RepositoryError, ChallengeItem?> =
        mongoCatch {
            collection
                .find(
                    Filters.and(
                        Filters.eq(ChallengeItem::id, id),
                        Filters.eq(ChallengeItem::archived, false),
                    ),
                ).toList()
                .firstOrNull()
        }

    suspend fun create(item: ChallengeItem): Either<RepositoryError, ChallengeItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun update(
        id: ObjectId,
        request: UpsertChallengeRequest,
        now: String,
    ): Either<RepositoryError, ChallengeItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(
                    Filters.eq(ChallengeItem::id, id),
                    Filters.eq(ChallengeItem::archived, false),
                ),
                Updates.combine(
                    Updates.set(ChallengeItem::title, request.title.trim()),
                    Updates.set(ChallengeItem::description, request.description.trim()),
                    Updates.set(ChallengeItem::area, request.area),
                    Updates.set(ChallengeItem::municipalities, request.municipalities),
                    Updates.set(ChallengeItem::updatedAt, now),
                ),
                FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
            )
        }

    suspend fun softDelete(
        id: ObjectId,
        now: String,
    ): Either<RepositoryError, Boolean> =
        mongoCatch {
            val result =
                collection.updateOne(
                    Filters.and(
                        Filters.eq(ChallengeItem::id, id),
                        Filters.eq(ChallengeItem::archived, false),
                    ),
                    Updates.combine(
                        Updates.set(ChallengeItem::archived, true),
                        Updates.set(ChallengeItem::updatedAt, now),
                    ),
                )
            result.matchedCount > 0
        }
}
