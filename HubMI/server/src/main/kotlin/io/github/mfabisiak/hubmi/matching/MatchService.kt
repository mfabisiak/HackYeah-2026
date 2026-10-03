package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.MatchResult
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.mongo.matches
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.matching.toSimilarNeed
import java.time.Instant

class MatchService(
    private val engine: MatchingEngine,
    private val needs: NeedRepository,
) {
    /**
     * Matches the description against the innovations and stores it as a need. Personal data is removed first, so the
     * original text reaches neither the matcher, the database nor the logs. A logged-in [reporter] becomes the owner
     * of the need; anonymous needs have none.
     */
    suspend fun match(
        draft: MatchRequestDraft,
        reporter: UserId?,
    ): Either<DomainError, MatchResult> =
        either {
            val text = PersonalDataScrubber.scrub(draft.description.value)
            val municipality = draft.municipality?.let { PersonalDataScrubber.scrub(it.value) }
            val result = engine.match(text, draft.municipality).bind()
            val best = result.matches.firstOrNull()?.innovation
            val similar = best?.let { similarNeeds(it).bind() }.orEmpty()
            val need =
                NeedItem(
                    text = text,
                    ownerId = reporter?.value,
                    municipality = municipality,
                    areas = best?.areas.orEmpty(),
                    matchedInnovationIds = result.matches.map { it.innovation.id },
                    noGoodMatch = result.noGoodMatch,
                    createdAt = Instant.now().toString(),
                )
            needs.create(need).mapLeft { it.toDomainError() }.bind()
            MatchResult(
                needId = need.id.toHexString(),
                matches = result.matches.map { it.toDto() },
                similarNeeds = similar.map { it.toSimilarNeed(excerptOf(it.text)) },
                noGoodMatch = result.noGoodMatch,
            )
        }

    private suspend fun similarNeeds(best: InnovationItem): Either<DomainError, List<NeedItem>> =
        needs.findSimilar(best.id, SIMILAR_NEEDS_LIMIT).mapLeft { it.toDomainError() }

    /**
     * Stores the answer to "did it help?"; answering again overwrites the previous answer. Only the owner may answer
     * for a need that has one; an anonymous need is answered with its id alone.
     */
    suspend fun recordFeedback(
        id: NeedId,
        helpful: Boolean,
        caller: UserId?,
    ): Either<DomainError, Unit> =
        either {
            val need =
                ensureNotNull(needs.findById(id).mapLeft { it.toDomainError() }.bind()) {
                    DomainError.NotFound("Nie znaleziono zgłoszenia o ID: ${id.value.toHexString()}")
                }
            need.ownerId?.let { owner ->
                ensureNotNull(caller) { DomainError.Unauthorized() }
                ensure(caller.value == owner) { DomainError.Forbidden() }
            }
            needs.setHelpful(id, helpful).mapLeft { it.toDomainError() }.bind()
        }.map { }

    private companion object {
        const val SIMILAR_NEEDS_LIMIT = 3
    }
}
