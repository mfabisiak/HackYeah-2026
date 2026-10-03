package io.github.mfabisiak.hubmi.innovations

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.common.HttpUrl
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.SearchQuery
import io.github.mfabisiak.hubmi.common.mongo.SoftDeleteRepository
import io.github.mfabisiak.hubmi.common.mongo.hasElement
import io.github.mfabisiak.hubmi.common.mongo.matches
import io.github.mfabisiak.hubmi.common.mongo.matchesAny
import org.bson.conversions.Bson

const val INNOVATIONS_COLLECTION = "innovations"

val MongoDatabase.innovations: MongoCollection<InnovationItem>
    get() = getCollection<InnovationItem>(INNOVATIONS_COLLECTION)

class InnovationRepository(
    database: MongoDatabase,
) : SoftDeleteRepository<InnovationItem>(
        collection = database.innovations,
        idField = InnovationItem::id,
        archivedField = InnovationItem::archived,
        updatedAtField = InnovationItem::updatedAt,
    ) {
    suspend fun findAll(
        q: SearchQuery?,
        area: SocialArea?,
        targetGroup: TargetGroup?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<InnovationItem>> =
        findPage(
            filters =
                buildList<Bson> {
                    if (area != null) {
                        add(InnovationItem::areas.hasElement(area))
                    }
                    if (targetGroup != null) {
                        add(InnovationItem::targetGroups.hasElement(targetGroup))
                    }
                    if (q != null) {
                        add(
                            Filters.or(
                                q.matches(InnovationItem::title),
                                q.matches(InnovationItem::summary),
                                q.matches(InnovationItem::description),
                                q.matchesAny(InnovationItem::keywords),
                            ),
                        )
                    }
                },
            pageRequest = pageRequest,
        )

    suspend fun findById(id: InnovationId): Either<RepositoryError, InnovationItem?> = findActive(id.value)

    suspend fun update(
        id: InnovationId,
        draft: InnovationDraft,
        now: String,
    ): Either<RepositoryError, InnovationItem?> =
        updateActive(
            id.value,
            now,
            Updates.set(InnovationItem::title, draft.title.value),
            Updates.set(InnovationItem::summary, draft.summary.value),
            Updates.set(InnovationItem::description, draft.description.value),
            Updates.set(InnovationItem::areas, draft.areas.toList()),
            Updates.set(InnovationItem::targetGroups, draft.targetGroups.toList()),
            Updates.set(InnovationItem::stage, draft.stage),
            Updates.set(InnovationItem::region, draft.region?.value),
            Updates.set(InnovationItem::mediaUrls, draft.mediaUrls.map(HttpUrl::value)),
            Updates.set(InnovationItem::innovativeness, draft.narrative.innovativeness?.value),
            Updates.set(InnovationItem::problemDiagnosis, draft.narrative.problemDiagnosis?.value),
            Updates.set(InnovationItem::audienceDescription, draft.narrative.audienceDescription?.value),
            Updates.set(InnovationItem::expectedChange, draft.narrative.expectedChange?.value),
            Updates.set(InnovationItem::futureVision, draft.narrative.futureVision?.value),
        )

    suspend fun softDelete(
        id: InnovationId,
        now: String,
    ): Either<RepositoryError, Boolean> = archive(id.value, now)
}
