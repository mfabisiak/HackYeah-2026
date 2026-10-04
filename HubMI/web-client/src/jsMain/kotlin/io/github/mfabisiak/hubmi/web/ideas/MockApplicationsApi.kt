package io.github.mfabisiak.hubmi.web.ideas

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.mock.DEMO_USER_ID
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.MockStore
import io.github.mfabisiak.hubmi.web.mock.conflict
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.mock.statusNow
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlinx.serialization.json.Json
import kotlin.js.Promise

private val json = Json { ignoreUnknownKeys = true }

private const val BAD_REQUEST = 400

/** A draft application for the call, prefilled from the idea when there is one. Shared by `calls.apply` and `applications.createDraft`. */
internal fun Raise<ApiErrorJs>.draftApplication(
    store: MockStore,
    callId: String,
    ideaId: String?,
): ApplicationDto {
    val call = ensureNotNull(store.db.calls.firstOrNull { it.id == callId }) { notFound("nabór $callId") }.statusNow()
    val idea =
        ideaId?.let { id ->
            ensureNotNull(store.db.ideas.firstOrNull { it.idea.id == id }) { notFound("pomysł $id") }.idea
        }
    val now = nowIso()
    val application =
        ApplicationDto(
            id = newId("wniosek"),
            callId = call.id,
            applicantId = DEMO_USER_ID,
            ideaId = idea?.id,
            status = ApplicationStatus.DRAFT,
            title = idea?.title,
            description = idea?.essence,
            createdAt = now,
            updatedAt = now,
        )
    store.update { it.copy(applications = it.applications + application) }
    return application
}

internal class MockApplicationsApi(
    private val backend: MockBackend,
) : ApplicationsApi {
    private val store = backend.store

    override fun mine(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<ApplicationJs>>> =
        backend.respond {
            store.db.applications
                .filter { it.applicantId == DEMO_USER_ID }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun get(id: String): Promise<ApiResult<ApplicationJs>> = backend.respond { find(id).toJs() }

    override fun createDraft(
        callId: String,
        ideaId: String?,
    ): Promise<ApiResult<ApplicationJs>> = backend.respond { draftApplication(store, callId, ideaId).toJs() }

    override fun saveDraft(
        id: String,
        bodyJson: String,
    ): Promise<ApiResult<ApplicationJs>> =
        backend.respond {
            val request =
                Either
                    .catch { json.decodeFromString<SaveApplicationDraftRequest>(bodyJson) }
                    .mapLeft { ApiErrorJs(BAD_REQUEST, "INVALID_BODY", it.message ?: "Invalid JSON") }
                    .bind()
            val current = find(id)
            ensure(current.status == ApplicationStatus.DRAFT) { conflict("Złożony wniosek nie podlega edycji.") }
            val saved =
                current.copy(
                    title = request.title,
                    applicant = request.applicant,
                    description = request.description,
                    innovativeness = request.innovativeness,
                    problemDiagnosis = request.problemDiagnosis,
                    socialArea = request.socialArea,
                    audienceDescription = request.audienceDescription,
                    expectedChange = request.expectedChange,
                    futureVision = request.futureVision,
                    plan = request.plan,
                    requestedGrantAmountGrosze = request.requestedGrantAmountGrosze,
                    projectTeam = request.projectTeam,
                    declarations = request.declarations,
                    updatedAt = nowIso(),
                )
            replace(saved)
            saved.toJs()
        }

    override fun submit(id: String): Promise<ApiResult<ApplicationJs>> =
        backend.respond {
            val current = find(id)
            ensure(current.status == ApplicationStatus.DRAFT) { conflict("Wniosek został już złożony.") }
            val now = nowIso()
            val submitted = current.copy(status = ApplicationStatus.SUBMITTED, submittedAt = now, updatedAt = now)
            replace(submitted)
            submitted.toJs()
        }

    private fun Raise<ApiErrorJs>.find(id: String): ApplicationDto =
        ensureNotNull(store.db.applications.firstOrNull { it.id == id }) { notFound("wniosek $id") }

    private fun replace(application: ApplicationDto) =
        store.update { db ->
            db.copy(
                applications =
                    db.applications.map {
                        if (it.id ==
                            application.id
                        ) {
                            application
                        } else {
                            it
                        }
                    },
            )
        }
}
