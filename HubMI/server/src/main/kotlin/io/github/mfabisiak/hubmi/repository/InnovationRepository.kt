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
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.models.InnovationItem
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.util.regex.Pattern

class InnovationRepository(
    database: MongoDatabase,
) {
    private val collection = database.innovations

    suspend fun findAll(
        q: String?,
        area: SocialArea?,
        targetGroup: TargetGroup?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<InnovationItem>> =
        mongoCatch {
            val filters =
                buildList {
                    add(Filters.eq(InnovationItem::archived, false))
                    if (area != null) {
                        add(Filters.eq(InnovationItem::areas, area))
                    }
                    if (targetGroup != null) {
                        add(Filters.eq(InnovationItem::targetGroups, targetGroup))
                    }
                    if (!q.isNullOrBlank()) {
                        val pattern = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE)
                        add(
                            Filters.or(
                                Filters.regex(InnovationItem::title, pattern),
                                Filters.regex(InnovationItem::summary, pattern),
                                Filters.regex(InnovationItem::description, pattern),
                                Filters.regex(InnovationItem::keywords, pattern),
                            ),
                        )
                    }
                }
            val filter = Filters.and(filters)
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(InnovationItem::id))
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

    suspend fun findById(id: ObjectId): Either<RepositoryError, InnovationItem?> =
        mongoCatch {
            collection
                .find(
                    Filters.and(
                        Filters.eq(InnovationItem::id, id),
                        Filters.eq(InnovationItem::archived, false),
                    ),
                ).toList()
                .firstOrNull()
        }

    suspend fun create(item: InnovationItem): Either<RepositoryError, InnovationItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun update(
        id: ObjectId,
        request: UpsertInnovationRequest,
        now: String,
    ): Either<RepositoryError, InnovationItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(
                    Filters.eq(InnovationItem::id, id),
                    Filters.eq(InnovationItem::archived, false),
                ),
                Updates.combine(
                    Updates.set(InnovationItem::title, request.title.trim()),
                    Updates.set(InnovationItem::summary, request.summary.trim()),
                    Updates.set(InnovationItem::description, request.description.trim()),
                    Updates.set(InnovationItem::areas, request.areas),
                    Updates.set(InnovationItem::targetGroups, request.targetGroups),
                    Updates.set(InnovationItem::stage, request.stage),
                    Updates.set(InnovationItem::region, request.region?.trim()),
                    Updates.set(InnovationItem::mediaUrls, request.mediaUrls),
                    Updates.set(InnovationItem::updatedAt, now),
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
                        Filters.eq(InnovationItem::id, id),
                        Filters.eq(InnovationItem::archived, false),
                    ),
                    Updates.combine(
                        Updates.set(InnovationItem::archived, true),
                        Updates.set(InnovationItem::updatedAt, now),
                    ),
                )
            result.matchedCount > 0
        }
}
