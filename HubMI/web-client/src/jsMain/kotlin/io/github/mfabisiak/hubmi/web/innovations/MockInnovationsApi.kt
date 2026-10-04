package io.github.mfabisiak.hubmi.web.innovations

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.mock.DEMO_USER_ID
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.conflict
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.mock.stemsOf
import io.github.mfabisiak.hubmi.web.mock.toFeedback
import io.github.mfabisiak.hubmi.web.mock.toRequest
import io.github.mfabisiak.hubmi.web.mock.toSummary
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlin.js.Promise

private const val MIN_RATING = 1
private const val MAX_RATING = 5

internal class MockInnovationsApi(
    private val backend: MockBackend,
) : InnovationsApi {
    private val store = backend.store

    override fun list(
        q: String?,
        area: String?,
        targetGroup: String?,
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<InnovationSummaryJs>>> =
        backend.respond {
            val areaFilter = enumOrNull<SocialArea>(area, "area")
            val groupFilter = enumOrNull<TargetGroup>(targetGroup, "targetGroup")
            val queryStems = q?.let(::stemsOf).orEmpty()
            store.db.innovations
                .filter { areaFilter == null || areaFilter in it.areas }
                .filter { groupFilter == null || groupFilter in it.targetGroups }
                .filter {
                    queryStems.isEmpty() ||
                        stemsOf(it.title + " " + it.summary + " " + it.description).containsAll(queryStems)
                }.map { it.toSummary() }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun get(id: String): Promise<ApiResult<InnovationJs>> =
        backend.respond {
            ensureNotNull(
                store.db.innovationsWithRatings().firstOrNull { it.id == id },
            ) { notFound("innowacja $id") }.toJs()
        }

    override fun create(request: UpsertInnovationJs): Promise<ApiResult<InnovationJs>> =
        backend.respond {
            val innovation =
                request
                    .toDto()
                    .flatMap { it.validated() }
                    .bind()
                    .toInnovation(newId("innowacja"))
            store.update { it.copy(innovations = listOf(innovation) + it.innovations) }
            innovation.toJs()
        }

    override fun update(
        id: String,
        request: UpsertInnovationJs,
    ): Promise<ApiResult<InnovationJs>> =
        backend.respond {
            ensure(store.db.innovations.any { it.id == id }) { notFound("innowacja $id") }
            val innovation =
                request
                    .toDto()
                    .flatMap { it.validated() }
                    .bind()
                    .toInnovation(id)
            store.update { db -> db.copy(innovations = db.innovations.map { if (it.id == id) innovation else it }) }
            innovation.toJs()
        }

    override fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        backend.respond {
            ensure(store.db.innovations.any { it.id == id }) { notFound("innowacja $id") }
            store.update { db ->
                db.copy(
                    innovations = db.innovations.filterNot { it.id == id },
                    feedback = db.feedback.filterNot { it.innovationId == id },
                    testRequests = db.testRequests.filterNot { it.innovationId == id },
                )
            }
            EmptyJs()
        }

    override fun requestTest(
        id: String,
        note: String?,
    ): Promise<ApiResult<TestRequestJs>> =
        backend.respond {
            val innovation =
                ensureNotNull(store.db.innovations.firstOrNull { it.id == id }) { notFound("innowacja $id") }
            val existing = store.db.testRequests.firstOrNull { it.innovationId == id && it.userId == DEMO_USER_ID }
            ensure(existing == null || existing.status == TestRequestStatus.NEW) {
                conflict("Prośba o testy została już rozpatrzona przez administratora.")
            }
            val now = nowIso()
            val request =
                AdminTestRequestDto(
                    id = existing?.id ?: newId("test"),
                    innovationId = id,
                    innovationTitle = innovation.title,
                    userId = DEMO_USER_ID,
                    note = note?.trim()?.ifBlank { null },
                    status = TestRequestStatus.NEW,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            store.update { db -> db.copy(testRequests = db.testRequests.filterNot { it.id == request.id } + request) }
            request.toRequest().toJs()
        }

    override fun myTestRequest(id: String): Promise<ApiResult<TestRequestJs>> =
        backend.respond {
            ensureNotNull(store.db.testRequests.firstOrNull { it.innovationId == id && it.userId == DEMO_USER_ID }) {
                notFound("prośba o testy")
            }.toRequest().toJs()
        }

    override fun myFeedback(id: String): Promise<ApiResult<FeedbackJs>> =
        backend.respond {
            ensureNotNull(store.db.feedback.firstOrNull { it.innovationId == id && it.userId == DEMO_USER_ID }) {
                notFound("ocena")
            }.toFeedback().toJs()
        }

    override fun sendFeedback(
        id: String,
        rating: Int,
        comment: String?,
        suggestion: String?,
    ): Promise<ApiResult<FeedbackJs>> =
        backend.respond {
            val innovation =
                ensureNotNull(store.db.innovations.firstOrNull { it.id == id }) { notFound("innowacja $id") }
            ensure(rating in MIN_RATING..MAX_RATING) {
                invalid(
                    FieldError(
                        "rating",
                        FieldErrorCode.Range(MIN_RATING, MAX_RATING),
                        "Ocena musi mieścić się w zakresie 1–5",
                    ),
                )
            }
            val existing = store.db.feedback.firstOrNull { it.innovationId == id && it.userId == DEMO_USER_ID }
            val now = nowIso()
            val feedback =
                AdminFeedbackDto(
                    id = existing?.id ?: newId("ocena"),
                    innovationId = id,
                    innovationTitle = innovation.title,
                    userId = DEMO_USER_ID,
                    rating = rating,
                    comment = comment?.trim()?.ifBlank { null },
                    suggestion = suggestion?.trim()?.ifBlank { null },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            store.update { db -> db.copy(feedback = db.feedback.filterNot { it.id == feedback.id } + feedback) }
            feedback.toFeedback().toJs()
        }

    private fun UpsertInnovationRequest.validated(): Either<ApiErrorJs, UpsertInnovationRequest> {
        val errors =
            listOfNotNull(
                blank("title").takeIf { title.isBlank() },
                blank("summary").takeIf { summary.isBlank() },
                blank("description").takeIf { description.isBlank() },
            )
        return if (errors.isEmpty()) right() else invalid(*errors.toTypedArray()).left()
    }

    private fun UpsertInnovationRequest.toInnovation(id: String): InnovationDto =
        InnovationDto(
            id = id,
            title = title.trim(),
            summary = summary.trim(),
            description = description.trim(),
            areas = areas,
            targetGroups = targetGroups,
            stage = stage,
            region = region?.trim()?.ifBlank { null },
            mediaUrls = mediaUrls,
            averageRating = null,
            ratingsCount = 0,
            innovativeness = innovativeness,
            problemDiagnosis = problemDiagnosis,
            audienceDescription = audienceDescription,
            expectedChange = expectedChange,
            futureVision = futureVision,
        )
}
