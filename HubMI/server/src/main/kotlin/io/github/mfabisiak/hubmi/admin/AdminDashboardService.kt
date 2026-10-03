package io.github.mfabisiak.hubmi.admin

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.AdminSummaryDto
import io.github.mfabisiak.hubmi.api.AreaTrend
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.MonthlyTrendPoint
import io.github.mfabisiak.hubmi.api.MunicipalityTrend
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.TrendsDto
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.ideas.IdeaRepository
import io.github.mfabisiak.hubmi.matching.NeedItem
import io.github.mfabisiak.hubmi.matching.NeedRepository
import io.github.mfabisiak.hubmi.matching.TextAnalyzer
import io.github.mfabisiak.hubmi.matching.Token
import io.github.mfabisiak.hubmi.messaging.ThreadRepository
import io.github.mfabisiak.hubmi.tester.TestRequestRepository
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class AdminDashboardService(
    private val needRepository: NeedRepository,
    private val ideaRepository: IdeaRepository,
    private val testRequestRepository: TestRequestRepository,
    private val threadRepository: ThreadRepository,
    private val textAnalyzer: TextAnalyzer,
    private val clock: Clock = Clock.systemUTC(),
    private val privacyThreshold: Int = DEFAULT_PRIVACY_THRESHOLD,
) {
    suspend fun getTrends(monthsParam: Int): Either<DomainError, TrendsDto> =
        either {
            val months = monthsParam.coerceIn(1, 60)
            val now = clock.instant()
            val nowUtc = now.atZone(ZoneOffset.UTC)

            val currentWindowStart = nowUtc.minusMonths(months.toLong()).toInstant()
            val previousWindowStart = nowUtc.minusMonths(2L * months).toInstant()

            val needs =
                needRepository
                    .findSince(previousWindowStart.toString())
                    .mapLeft { it.toDomainError() }
                    .bind()

            val (currentNeeds, previousNeeds) =
                needs.partition { item ->
                    val itemInstant = parseInstantOrEpoch(item.createdAt)
                    itemInstant >= currentWindowStart && itemInstant <= now
                }

            val validPreviousNeeds =
                previousNeeds.filter { item ->
                    val itemInstant = parseInstantOrEpoch(item.createdAt)
                    itemInstant >= previousWindowStart && itemInstant < currentWindowStart
                }

            val currentAreaCounts = countByArea(currentNeeds)
            val previousAreaCounts = countByArea(validPreviousNeeds)
            val byArea =
                SocialArea.entries
                    .map { area ->
                        AreaTrend(
                            area = area,
                            count = currentAreaCounts[area] ?: 0,
                            previousCount = previousAreaCounts[area] ?: 0,
                        )
                    }.sortedWith(compareByDescending<AreaTrend> { it.count }.thenBy { it.area.name })

            val byMunicipality =
                currentNeeds
                    .mapNotNull { it.municipality?.trim()?.takeIf(String::isNotBlank) }
                    .groupingBy { it }
                    .eachCount()
                    .filter { (_, count) -> count >= privacyThreshold }
                    .map { (municipality, count) -> MunicipalityTrend(municipality = municipality, count = count) }
                    .sortedWith(compareByDescending<MunicipalityTrend> { it.count }.thenBy { it.municipality })

            val unmatchedNeedsInWindow = currentNeeds.filter { isUnmatched(it) }
            val unmatchedNeedsCount = unmatchedNeedsInWindow.size

            val currentYearMonth = YearMonth.from(nowUtc)
            val seriesMonths =
                (0 until months).map { offset ->
                    currentYearMonth.minusMonths((months - 1 - offset).toLong())
                }
            val seriesCounts =
                currentNeeds
                    .groupingBy { item ->
                        val instant = parseInstantOrEpoch(item.createdAt)
                        YearMonth.from(instant.atZone(ZoneOffset.UTC))
                    }.eachCount()

            val series =
                seriesMonths.map { ym ->
                    MonthlyTrendPoint(
                        month = ym.toString(),
                        count = seriesCounts[ym] ?: 0,
                    )
                }

            val topUnmatchedTerms = extractTopUnmatchedTerms(unmatchedNeedsInWindow)

            TrendsDto(
                byArea = byArea,
                byMunicipality = byMunicipality,
                unmatchedNeeds = unmatchedNeedsCount,
                series = series,
                topUnmatchedTerms = topUnmatchedTerms,
            )
        }

    suspend fun getSummary(): Either<DomainError, AdminSummaryDto> =
        either {
            val now = clock.instant()
            val oneWeekAgo = now.minus(7, ChronoUnit.DAYS).toString()

            val submittedIdeas =
                ideaRepository
                    .countByStatus(IdeaStatus.SUBMITTED)
                    .mapLeft { it.toDomainError() }
                    .bind()

            val pendingTestRequests =
                testRequestRepository
                    .countByStatus(TestRequestStatus.NEW)
                    .mapLeft { it.toDomainError() }
                    .bind()

            val unmatchedNeedsThisWeek =
                needRepository
                    .countUnmatchedSince(oneWeekAgo)
                    .mapLeft { it.toDomainError() }
                    .bind()

            val pendingThreads =
                threadRepository
                    .countPendingAdminReply()
                    .mapLeft { it.toDomainError() }
                    .bind()

            AdminSummaryDto(
                submittedIdeas = submittedIdeas,
                pendingTestRequests = pendingTestRequests,
                unmatchedNeedsThisWeek = unmatchedNeedsThisWeek,
                pendingThreads = pendingThreads,
            )
        }

    private fun countByArea(items: List<NeedItem>): Map<SocialArea, Int> =
        items
            .flatMap { item -> item.areas.ifEmpty { listOf(SocialArea.OTHER) } }
            .groupingBy { it }
            .eachCount()

    private fun isUnmatched(item: NeedItem): Boolean = item.noGoodMatch || item.matchedInnovationIds.isEmpty()

    private fun extractTopUnmatchedTerms(unmatched: List<NeedItem>): List<String> {
        val allTokens = unmatched.flatMap { textAnalyzer.analyze(it.text) }
        return allTokens
            .groupBy { it.stem }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, List<Token>>> { it.value.size }.thenBy { it.key })
            .take(TOP_TERMS_LIMIT)
            .map { (_, tokens) ->
                tokens
                    .groupingBy { it.surface }
                    .eachCount()
                    .entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    .first()
                    .key
            }
    }

    private fun parseInstantOrEpoch(raw: String): Instant =
        Either.catch { Instant.parse(raw) }.getOrElse { Instant.EPOCH }

    companion object {
        const val DEFAULT_PRIVACY_THRESHOLD = 3
        const val TOP_TERMS_LIMIT = 10
    }
}
