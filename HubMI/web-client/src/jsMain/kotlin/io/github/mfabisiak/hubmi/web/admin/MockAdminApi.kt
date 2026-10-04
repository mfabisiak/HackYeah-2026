package io.github.mfabisiak.hubmi.web.admin

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.AreaTrend
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.MonthlyTrendPoint
import io.github.mfabisiak.hubmi.api.MunicipalityTrend
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.TrendsDto
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.adaptations.AdaptationJs
import io.github.mfabisiak.hubmi.web.adaptations.toJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.ideas.ApplicationJs
import io.github.mfabisiak.hubmi.web.ideas.IdeaJs
import io.github.mfabisiak.hubmi.web.ideas.toJs
import io.github.mfabisiak.hubmi.web.mock.DEMO_USER_ID
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.conflict
import io.github.mfabisiak.hubmi.web.mock.epochMillis
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.isoDaysFromNow
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlin.js.Date
import kotlin.js.Promise

private const val PRIVACY_THRESHOLD = 3
private const val WEEK_DAYS = 7
private const val MONTHS_IN_YEAR = 12
private const val MAX_MONTHS = 24

/** The admin side of the demo; the interface decides who gets to see it, the mock refuses nobody. */
internal class MockAdminApi(
    private val backend: MockBackend,
) : AdminApi {
    private val store = backend.store

    override fun trends(months: Int): Promise<ApiResult<TrendsJs>> =
        backend.respond {
            val unmatched = store.db.needs.count { !it.matched }
            TrendsDto(
                byArea =
                    listOf(
                        AreaTrend(SocialArea.AGING, 42, 35),
                        AreaTrend(SocialArea.LONELINESS, 31, 28),
                        AreaTrend(SocialArea.MENTAL_HEALTH, 27, 19),
                        AreaTrend(SocialArea.SERVICE_ACCESS, 18, 20),
                        AreaTrend(SocialArea.DIGITAL_EXCLUSION, 15, 9),
                        AreaTrend(SocialArea.COORDINATION, 9, 10),
                        AreaTrend(SocialArea.DEPOPULATION, 7, 4),
                    ),
                byMunicipality =
                    listOf(
                        MunicipalityTrend("Kraków", 38),
                        MunicipalityTrend("Tarnów", 17),
                        MunicipalityTrend("Nowy Sącz", 14),
                        MunicipalityTrend("Wieliczka", 11),
                        MunicipalityTrend("Oświęcim", 9),
                        MunicipalityTrend("Zakopane", 6),
                    ),
                unmatchedNeeds = unmatched + 15,
                series = monthlySeries(months.coerceIn(1, MAX_MONTHS)),
                topUnmatchedTerms =
                    listOf(
                        "opieka wytchnieniowa",
                        "tłumacz migowy",
                        "transport nocny",
                        "bezdomność",
                        "autyzm",
                    ),
                privacyThreshold = PRIVACY_THRESHOLD,
            ).toJs()
        }

    override fun summary(): Promise<ApiResult<AdminSummaryJs>> =
        backend.respond {
            val db = store.db
            val weekAgo = Date.now() - WEEK_DAYS * DAY_MS
            AdminSummaryJs(
                submittedIdeas = db.ideas.count { it.idea.status == IdeaStatus.SUBMITTED },
                pendingTestRequests = db.testRequests.count { it.status == TestRequestStatus.NEW },
                unmatchedNeedsThisWeek = db.needs.count { !it.matched && epochMillis(it.createdAt) >= weekAgo },
                pendingThreads =
                    db.threads.count {
                        it.messages.maxByOrNull { message -> message.sentAt }?.authorRole ==
                            ParticipantRole.AUTHOR
                    },
            )
        }

    override fun ideas(
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<IdeaJs>>> =
        backend.respond {
            val statusFilter = enumOrNull<IdeaStatus>(status, "status")
            store.db.ideas
                .map { it.idea }
                .filter { statusFilter == null || it.status == statusFilter }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun updateIdeaStatus(
        id: String,
        status: String,
        comment: String?,
    ): Promise<ApiResult<IdeaJs>> =
        backend.respond {
            val newStatus = enumOf<IdeaStatus>(status, "status")
            val current = ensureNotNull(store.db.ideas.firstOrNull { it.idea.id == id }) { notFound("pomysł $id") }
            val updated = current.idea.copy(status = newStatus, adminComment = comment?.trim()?.ifBlank { null })
            val notification =
                NotificationDto(
                    id = newId("powiadomienie"),
                    type = NotificationType.IDEA_STATUS_CHANGED,
                    title = "Zmiana statusu pomysłu",
                    body = "Pomysł „${updated.title}” ma nowy status: ${statusLabel(newStatus)}.",
                    createdAt = nowIso(),
                    read = false,
                )
            store.update { db ->
                db.copy(
                    ideas = db.ideas.map { if (it.idea.id == id) it.copy(idea = updated) else it },
                    notifications =
                        if (current.authorId ==
                            DEMO_USER_ID
                        ) {
                            db.notifications + notification
                        } else {
                            db.notifications
                        },
                )
            }
            updated.toJs()
        }

    override fun applications(
        callId: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<ApplicationJs>>> =
        backend.respond {
            val statusFilter = enumOrNull<ApplicationStatus>(status, "status")
            store.db.applications
                .filter { callId == null || it.callId == callId }
                .filter { statusFilter == null || it.status == statusFilter }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun feedback(
        innovationId: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdminFeedbackJs>>> =
        backend.respond {
            store.db.feedback
                .filter { innovationId == null || it.innovationId == innovationId }
                .sortedByDescending { it.updatedAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun testRequests(
        innovationId: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdminTestRequestJs>>> =
        backend.respond {
            val statusFilter = enumOrNull<TestRequestStatus>(status, "status")
            store.db.testRequests
                .filter { innovationId == null || it.innovationId == innovationId }
                .filter { statusFilter == null || it.status == statusFilter }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun decideTestRequest(
        id: String,
        status: String,
    ): Promise<ApiResult<AdminTestRequestJs>> =
        backend.respond {
            val decision = enumOf<TestRequestStatus>(status, "status")
            ensure(decision != TestRequestStatus.NEW) { invalid(blank("status")) }
            val current =
                ensureNotNull(store.db.testRequests.firstOrNull { it.id == id }) { notFound("prośba o testy $id") }
            ensure(current.status == TestRequestStatus.NEW) { conflict("Prośba o testy została już rozpatrzona.") }
            val decided = current.copy(status = decision, updatedAt = nowIso())
            store.update { db -> db.copy(testRequests = db.testRequests.map { if (it.id == id) decided else it }) }
            decided.toJs()
        }

    override fun adaptations(
        status: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdaptationJs>>> =
        backend.respond {
            val statusFilter = enumOrNull<AdaptationStatus>(status, "status")
            store.db.adaptations
                .map { it.adaptation }
                .filter { statusFilter == null || it.status == statusFilter }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun reviewAdaptation(
        id: String,
        status: String,
        comment: String?,
    ): Promise<ApiResult<AdaptationJs>> =
        backend.respond {
            val decision = enumOf<AdaptationStatus>(status, "status")
            ensure(decision != AdaptationStatus.PENDING_REVIEW) { invalid(blank("status")) }
            ensure(decision != AdaptationStatus.REJECTED || !comment.isNullOrBlank()) { invalid(blank("comment")) }
            val current =
                ensureNotNull(store.db.adaptations.firstOrNull { it.adaptation.id == id }) { notFound("plan $id") }
            ensure(
                current.adaptation.status == AdaptationStatus.PENDING_REVIEW,
            ) { conflict("Plan został już zrecenzowany.") }
            val reviewed = current.adaptation.copy(status = decision, adminComment = comment?.trim()?.ifBlank { null })
            store.update { db ->
                db.copy(
                    adaptations =
                        db.adaptations.map {
                            if (it.adaptation.id ==
                                id
                            ) {
                                it.copy(adaptation = reviewed)
                            } else {
                                it
                            }
                        },
                )
            }
            reviewed.toJs()
        }

    private fun statusLabel(status: IdeaStatus): String =
        when (status) {
            IdeaStatus.DRAFT -> "wersja robocza"
            IdeaStatus.SUBMITTED -> "zgłoszony"
            IdeaStatus.IN_REVIEW -> "w przeglądzie"
            IdeaStatus.ACCEPTED -> "zaakceptowany"
            IdeaStatus.REJECTED -> "odrzucony"
        }

    /** The last [months] months up to the current one, as `YYYY-MM`, with a plausible number of needs in each. */
    private fun monthlySeries(months: Int): List<MonthlyTrendPoint> {
        val today = Date()
        val current = today.getFullYear() * MONTHS_IN_YEAR + today.getMonth()
        return (months - 1 downTo 0).map { back ->
            val index = current - back
            val month = index % MONTHS_IN_YEAR + 1
            MonthlyTrendPoint(
                "${index / MONTHS_IN_YEAR}-${month.toString().padStart(2, '0')}",
                MONTHLY_BASE + (months - back) * MONTHLY_GROWTH + month % 3 * 2,
            )
        }
    }

    private companion object {
        const val DAY_MS = 86_400_000.0
        const val MONTHLY_BASE = 8
        const val MONTHLY_GROWTH = 2
    }
}
