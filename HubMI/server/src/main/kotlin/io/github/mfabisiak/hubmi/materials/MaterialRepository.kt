package io.github.mfabisiak.hubmi.materials

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.SearchQuery
import io.github.mfabisiak.hubmi.common.mongo.SoftDeleteRepository
import io.github.mfabisiak.hubmi.common.mongo.hasElement
import io.github.mfabisiak.hubmi.common.mongo.matches
import org.bson.conversions.Bson

const val MATERIALS_COLLECTION = "materials"

val MongoDatabase.materials: MongoCollection<MaterialItem>
    get() = getCollection<MaterialItem>(MATERIALS_COLLECTION)

class MaterialRepository(
    database: MongoDatabase,
) : SoftDeleteRepository<MaterialItem>(
        collection = database.materials,
        idField = MaterialItem::id,
        archivedField = MaterialItem::archived,
        updatedAtField = MaterialItem::updatedAt,
    ) {
    suspend fun findAll(
        q: SearchQuery?,
        area: SocialArea?,
        type: MaterialType?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<MaterialItem>> =
        findPage(
            filters =
                buildList<Bson> {
                    if (area != null) add(MaterialItem::areas.hasElement(area))
                    if (type != null) add(Filters.eq(MaterialItem::type, type))
                    if (q != null) add(Filters.or(q.matches(MaterialItem::title), q.matches(MaterialItem::description)))
                },
            pageRequest = pageRequest,
        )

    suspend fun findById(id: MaterialId): Either<RepositoryError, MaterialItem?> = findActive(id.value)

    suspend fun update(
        id: MaterialId,
        draft: MaterialDraft,
        now: String,
    ): Either<RepositoryError, MaterialItem?> =
        updateActive(
            id.value,
            now,
            Updates.set(MaterialItem::title, draft.title.value),
            Updates.set(MaterialItem::description, draft.description.value),
            Updates.set(MaterialItem::type, draft.type),
            Updates.set(MaterialItem::url, draft.url.value),
            Updates.set(MaterialItem::areas, draft.areas.toList()),
        )

    suspend fun softDelete(
        id: MaterialId,
        now: String,
    ): Either<RepositoryError, Boolean> = archive(id.value, now)
}
