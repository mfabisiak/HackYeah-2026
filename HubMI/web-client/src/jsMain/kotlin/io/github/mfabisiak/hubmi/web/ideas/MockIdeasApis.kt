package io.github.mfabisiak.hubmi.web.ideas

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.right
import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.RopsDeclarations
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.enumOrNull
import io.github.mfabisiak.hubmi.web.mock.DEMO_USER_ID
import io.github.mfabisiak.hubmi.web.mock.MockBackend
import io.github.mfabisiak.hubmi.web.mock.MockIdea
import io.github.mfabisiak.hubmi.web.mock.blank
import io.github.mfabisiak.hubmi.web.mock.invalid
import io.github.mfabisiak.hubmi.web.mock.newId
import io.github.mfabisiak.hubmi.web.mock.notFound
import io.github.mfabisiak.hubmi.web.mock.nowIso
import io.github.mfabisiak.hubmi.web.mock.pageOf
import io.github.mfabisiak.hubmi.web.mock.statusNow
import io.github.mfabisiak.hubmi.web.toPageJs
import kotlin.js.Promise

internal class MockIdeasApi(
    private val backend: MockBackend,
) : IdeasApi {
    private val store = backend.store

    override fun create(request: CreateIdeaJs): Promise<ApiResult<IdeaJs>> =
        backend.respond {
            val draft = request.toDto().flatMap { it.validated() }.bind()
            val idea =
                IdeaDto(
                    id = newId("pomysl"),
                    title = draft.title.trim(),
                    essence = draft.essence.trim(),
                    targetGroups = draft.targetGroups,
                    stage = draft.stage,
                    status = IdeaStatus.SUBMITTED,
                    adminComment = null,
                    createdAt = nowIso(),
                )
            val notification =
                NotificationDto(
                    id = newId("powiadomienie"),
                    type = NotificationType.IDEA_SUBMITTED,
                    title = "Pomysł zgłoszony",
                    body = "Pomysł „${idea.title}” czeka na przegląd zespołu Hubu.",
                    createdAt = idea.createdAt,
                    read = false,
                )
            store.update {
                it.copy(
                    ideas = it.ideas + MockIdea(idea, DEMO_USER_ID),
                    notifications =
                        it.notifications + notification,
                )
            }
            idea.toJs()
        }

    override fun mine(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<IdeaJs>>> =
        backend.respond {
            store.db.ideas
                .filter { it.authorId == DEMO_USER_ID }
                .map { it.idea }
                .sortedByDescending { it.createdAt }
                .pageOf(page, size)
                .toPageJs { it.toJs() }
        }

    override fun get(id: String): Promise<ApiResult<IdeaJs>> =
        backend.respond {
            ensureNotNull(store.db.ideas.firstOrNull { it.idea.id == id }) { notFound("pomysł $id") }.idea.toJs()
        }

    private fun CreateIdeaRequest.validated(): Either<ApiErrorJs, CreateIdeaRequest> {
        val errors =
            listOfNotNull(
                blank("title").takeIf { title.isBlank() },
                blank("essence").takeIf { essence.isBlank() },
            )
        return if (errors.isEmpty()) right() else invalid(*errors.toTypedArray()).left()
    }
}

internal class MockCallsApi(
    private val backend: MockBackend,
) : CallsApi {
    private val store = backend.store

    override fun list(status: String?): Promise<ApiResult<Array<GrantCallJs>>> =
        backend.respond {
            val statusFilter = enumOrNull<CallStatus>(status, "status")
            store.db.calls
                .map { it.statusNow() }
                .filter { statusFilter == null || it.status == statusFilter }
                .map { it.toJs() }
                .toTypedArray()
        }

    override fun active(): Promise<ApiResult<Array<GrantCallJs>>> =
        backend.respond {
            store.db.calls
                .map { it.statusNow() }
                .filter { it.status == CallStatus.OPEN }
                .map { it.toJs() }
                .toTypedArray()
        }

    override fun get(id: String): Promise<ApiResult<GrantCallJs>> =
        backend.respond {
            ensureNotNull(store.db.calls.firstOrNull { it.id == id }) { notFound("nabór $id") }.statusNow().toJs()
        }

    override fun declarations(
        id: String,
        applicantType: String?,
    ): Promise<ApiResult<DeclarationsJs>> =
        backend.respond {
            ensure(store.db.calls.any { it.id == id }) { notFound("nabór $id") }
            RopsDeclarations.getDeclarations(enumOrNull<ApplicantType>(applicantType, "applicantType")).toJs()
        }

    override fun create(request: UpsertCallJs): Promise<ApiResult<GrantCallJs>> =
        backend.respond {
            val call = request.toDto().toCall(newId("nabor"))
            store.update { it.copy(calls = listOf(call) + it.calls) }
            call.statusNow().toJs()
        }

    override fun update(
        id: String,
        request: UpsertCallJs,
    ): Promise<ApiResult<GrantCallJs>> =
        backend.respond {
            ensure(store.db.calls.any { it.id == id }) { notFound("nabór $id") }
            val call = request.toDto().toCall(id)
            store.update { db -> db.copy(calls = db.calls.map { if (it.id == id) call else it }) }
            call.statusNow().toJs()
        }

    override fun delete(id: String): Promise<ApiResult<EmptyJs>> =
        backend.respond {
            ensure(store.db.calls.any { it.id == id }) { notFound("nabór $id") }
            store.update { db -> db.copy(calls = db.calls.filterNot { it.id == id }) }
            EmptyJs()
        }

    override fun apply(
        callId: String,
        ideaId: String?,
    ): Promise<ApiResult<ApplicationJs>> = backend.respond { draftApplication(store, callId, ideaId).toJs() }

    private fun UpsertCallRequest.toCall(id: String) =
        GrantCallDto(id, title.trim(), description.trim(), opensAt, closesAt, CallStatus.UPCOMING, fields)
}
