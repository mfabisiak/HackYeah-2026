package io.github.mfabisiak.hubmi.web.matching

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.MatchDto
import io.github.mfabisiak.hubmi.api.MatchResult
import io.github.mfabisiak.hubmi.api.SimilarNeedDto
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.mock.DemoSeed
import io.github.mfabisiak.hubmi.web.mock.MatchIndex
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.MockNeed
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.stemsOf
import io.github.mfabisiak.hubmi.web.mock.toSummary
import kotlin.js.Promise

private const val EXCERPT_LENGTH = 140
private const val MAX_SIMILAR_NEEDS = 3
private const val MIN_SHARED_STEMS = 2

internal class MockMatchesApi(
    private val backend: MockBackend,
) : MatchesApi {
    private val store = backend.store

    override fun match(
        description: String,
        municipality: String?,
    ): Promise<ApiResult<MatchResultJs>> =
        backend.respond {
            ensure(description.isNotBlank()) { invalid(blank("description")) }
            val db = store.db
            val hits =
                MatchIndex(db.innovationsWithRatings(), DemoSeed.keywords)
                    .search(description, municipality)
                    .filter { it.relevance >= MatchIndex.GOOD_MATCH }
            val stems = stemsOf(description)
            val similarNeeds =
                db.needs
                    .filter { (stemsOf(it.description) intersect stems).size >= MIN_SHARED_STEMS }
                    .take(MAX_SIMILAR_NEEDS)
                    .map { SimilarNeedDto(it.id, it.description.take(EXCERPT_LENGTH), it.area) }
            val need =
                MockNeed(
                    id = newId("potrzeba"),
                    description = description.trim(),
                    municipality = municipality?.trim()?.ifBlank { null },
                    area =
                        hits
                            .firstOrNull()
                            ?.innovation
                            ?.areas
                            ?.firstOrNull(),
                    matched = hits.isNotEmpty(),
                    helpful = null,
                    createdAt = nowIso(),
                )
            store.update { it.copy(needs = it.needs + need) }
            MatchResult(
                needId = need.id,
                matches = hits.map { MatchDto(it.innovation.toSummary(), it.relevance, it.reasons, it.matchedTerms) },
                similarNeeds = similarNeeds,
                noGoodMatch = hits.isEmpty(),
            ).toJs()
        }

    override fun sendFeedback(
        needId: String,
        helpful: Boolean,
    ): Promise<ApiResult<EmptyJs>> =
        backend.respond {
            ensureNotNull(store.db.needs.firstOrNull { it.id == needId }) { notFound("potrzeba $needId") }
            store.update { db ->
                db.copy(
                    needs =
                        db.needs.map {
                            if (it.id ==
                                needId
                            ) {
                                it.copy(helpful = helpful)
                            } else {
                                it
                            }
                        },
                )
            }
            EmptyJs()
        }
}
