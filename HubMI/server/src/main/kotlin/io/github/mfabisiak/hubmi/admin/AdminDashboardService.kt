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
import io.github.mfabisiak.hubmi.matching.NeedTrendRow
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
    suspend fun getTrends(months: TrendsMonths): Either<DomainError, TrendsDto> =
        either {
            val currentYearMonth = YearMonth.from(clock.instant().atZone(ZoneOffset.UTC))
            val currentWindowStart = currentYearMonth.minusMonths(months.value - 1L).startInstant()
            val previousWindowStart = currentYearMonth.minusMonths(2L * months.value - 1).startInstant()

            val rows =
                needRepository
                    .findTrendRowsSince(previousWindowStart.toString())
                    .mapLeft { it.toDomainError() }
                    .bind()
            val unmatchedNeeds =
                needRepository
                    .findUnmatchedSince(currentWindowStart.toString())
                    .mapLeft { it.toDomainError() }
                    .bind()

            val (currentNeeds, previousNeeds) = rows.partition { it.createdAtInstant() >= currentWindowStart }

            val currentAreaCounts = countByArea(currentNeeds)
            val previousAreaCounts = countByArea(previousNeeds)
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

            val seriesCounts =
                currentNeeds
                    .groupingBy { YearMonth.from(it.createdAtInstant().atZone(ZoneOffset.UTC)) }
                    .eachCount()
            val series =
                (months.value - 1 downTo 0).map { monthsAgo ->
                    val month = currentYearMonth.minusMonths(monthsAgo.toLong())
                    MonthlyTrendPoint(month = month.toString(), count = seriesCounts[month] ?: 0)
                }

            TrendsDto(
                byArea = byArea,
                byMunicipality = byMunicipality,
                unmatchedNeeds = unmatchedNeeds.size,
                series = series,
                topUnmatchedTerms = topUnmatchedTerms(unmatchedNeeds),
                privacyThreshold = privacyThreshold,
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
                    .countPendingStaffReply()
                    .mapLeft { it.toDomainError() }
                    .bind()

            AdminSummaryDto(
                submittedIdeas = submittedIdeas,
                pendingTestRequests = pendingTestRequests,
                unmatchedNeedsThisWeek = unmatchedNeedsThisWeek,
                pendingThreads = pendingThreads,
            )
        }

    private fun countByArea(rows: List<NeedTrendRow>): Map<SocialArea, Int> =
        rows
            .flatMap { row -> row.areas.ifEmpty { listOf(SocialArea.OTHER) } }
            .groupingBy { it }
            .eachCount()

    /** Only words used in at least [privacyThreshold] different needs, so a single report never shows through. */
    private fun topUnmatchedTerms(unmatched: List<NeedItem>): List<String> =
        unmatched
            .flatMap { need -> textAnalyzer.analyze(need.text).distinctBy(Token::stem) }
            .groupBy(Token::stem)
            .filterValues { it.size >= privacyThreshold }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, List<Token>>> { it.value.size }.thenBy { it.key })
            .take(TOP_TERMS_LIMIT)
            .map { mostCommonSurface(it.value) }

    private fun mostCommonSurface(tokens: List<Token>): String =
        tokens
            .groupingBy(Token::surface)
            .eachCount()
            .entries
            .minWithOrNull(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            ?.key
            .orEmpty()

    private fun YearMonth.startInstant(): Instant = atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    private fun NeedTrendRow.createdAtInstant(): Instant =
        Either.catch { Instant.parse(createdAt) }.getOrElse { Instant.EPOCH }

    companion object {
        const val DEFAULT_PRIVACY_THRESHOLD = 3
        const val TOP_TERMS_LIMIT = 10
    }
}
