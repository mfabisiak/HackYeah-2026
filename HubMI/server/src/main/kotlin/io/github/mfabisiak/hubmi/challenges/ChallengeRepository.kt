package io.github.mfabisiak.hubmi.challenges

import arrow.core.Either
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.mongodb.kotlin.client.model.Filters
import com.mongodb.kotlin.client.model.Updates
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.common.RepositoryError
import io.github.mfabisiak.hubmi.common.mongo.SoftDeleteRepository

const val CHALLENGES_COLLECTION = "challenges"

val MongoDatabase.challenges: MongoCollection<ChallengeItem>
    get() = getCollection<ChallengeItem>(CHALLENGES_COLLECTION)

class ChallengeRepository(
    database: MongoDatabase,
) : SoftDeleteRepository<ChallengeItem>(
        collection = database.challenges,
        idField = ChallengeItem::id,
        archivedField = ChallengeItem::archived,
        updatedAtField = ChallengeItem::updatedAt,
    ) {
    suspend fun findAll(
        area: SocialArea?,
        pageRequest: PageRequest,
    ): Either<RepositoryError, Page<ChallengeItem>> =
        findPage(listOfNotNull(area?.let { Filters.eq(ChallengeItem::area, it) }), pageRequest)

    suspend fun findById(id: ChallengeId): Either<RepositoryError, ChallengeItem?> = findActive(id.value)

    suspend fun update(
        id: ChallengeId,
        draft: ChallengeDraft,
        now: String,
    ): Either<RepositoryError, ChallengeItem?> =
        updateActive(
            id.value,
            now,
            Updates.set(ChallengeItem::title, draft.title.value),
            Updates.set(ChallengeItem::description, draft.description.value),
            Updates.set(ChallengeItem::area, draft.area),
            Updates.set(ChallengeItem::municipalities, draft.municipalities.map(MunicipalityName::value)),
        )

    suspend fun softDelete(
        id: ChallengeId,
        now: String,
    ): Either<RepositoryError, Boolean> = archive(id.value, now)
}
