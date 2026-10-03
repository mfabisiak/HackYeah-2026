package io.github.mfabisiak.hubmi.repository

import arrow.core.Either
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Sorts
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.models.MaterialItem
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.util.regex.Pattern

class MaterialRepository(
    database: MongoDatabase,
) {
    private val collection = database.materials

    suspend fun findAll(
        q: String?,
        area: SocialArea?,
        type: MaterialType?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<MaterialItem>> =
        mongoCatch {
            val filters =
                buildList {
                    add(Filters.eq(MaterialItem::archived, false))
                    if (area != null) {
                        add(Filters.eq(MaterialItem::areas, area))
                    }
                    if (type != null) {
                        add(Filters.eq(MaterialItem::type, type))
                    }
                    if (!q.isNullOrBlank()) {
                        val pattern = Pattern.compile(Pattern.quote(q.trim()), Pattern.CASE_INSENSITIVE)
                        add(
                            Filters.or(
                                Filters.regex(MaterialItem::title, pattern),
                                Filters.regex(MaterialItem::description, pattern),
                            ),
                        )
                    }
                }
            val filter = Filters.and(filters)
            val total = collection.countDocuments(filter).toInt()
            val items =
                collection
                    .find(filter)
                    .sort(Sorts.descending(MaterialItem::id))
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

    suspend fun findById(id: ObjectId): Either<RepositoryError, MaterialItem?> =
        mongoCatch {
            collection
                .find(
                    Filters.and(
                        Filters.eq(MaterialItem::id, id),
                        Filters.eq(MaterialItem::archived, false),
                    ),
                ).toList()
                .firstOrNull()
        }

    suspend fun create(item: MaterialItem): Either<RepositoryError, MaterialItem> =
        mongoCatch {
            collection.insertOne(item)
            item
        }

    suspend fun update(
        id: ObjectId,
        request: UpsertMaterialRequest,
        now: String,
    ): Either<RepositoryError, MaterialItem?> =
        mongoCatch {
            collection.findOneAndUpdate(
                Filters.and(
                    Filters.eq(MaterialItem::id, id),
                    Filters.eq(MaterialItem::archived, false),
                ),
                Updates.combine(
                    Updates.set(MaterialItem::title, request.title.trim()),
                    Updates.set(MaterialItem::description, request.description.trim()),
                    Updates.set(MaterialItem::type, request.type),
                    Updates.set(MaterialItem::url, request.url.trim()),
                    Updates.set(MaterialItem::areas, request.areas),
                    Updates.set(MaterialItem::updatedAt, now),
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
                        Filters.eq(MaterialItem::id, id),
                        Filters.eq(MaterialItem::archived, false),
                    ),
                    Updates.combine(
                        Updates.set(MaterialItem::archived, true),
                        Updates.set(MaterialItem::updatedAt, now),
                    ),
                )
            result.matchedCount > 0
        }
}
