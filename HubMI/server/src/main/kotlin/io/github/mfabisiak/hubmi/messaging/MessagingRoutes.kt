package io.github.mfabisiak.hubmi.messaging

import io.github.mfabisiak.hubmi.api.CreateThreadRequest
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.PostMessageRequest
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.currentUser
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.ideas.CallChanged
import io.github.mfabisiak.hubmi.ideas.CallPublished
import io.github.mfabisiak.hubmi.ideas.IdeaStatusChanged
import io.github.mfabisiak.hubmi.ideas.IdeaSubmitted
import io.github.mfabisiak.hubmi.ideas.MessageReceived
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.filter
import org.koin.ktor.ext.inject

fun Route.messagingRoutes() {
    val threadService by inject<ThreadService>()
    val notificationService by inject<NotificationService>()
    val eventBus by inject<EventBus>()

    authenticate(KEYCLOAK_AUTH) {
        get<Threads> { params ->
            respondEither {
                val user = call.currentUser.bind()
                threadService.listThreads(user.id, user.roles, params.page, params.size).bind()
            }
        }

        post<Threads> {
            respondEither(HttpStatusCode.Created) {
                val user = call.currentUser.bind()
                val request = call.receive<CreateThreadRequest>()
                val dto =
                    threadService
                        .createThread(
                            callerId = user.id,
                            callerName = user.username ?: "Użytkownik",
                            callerRoles = user.roles,
                            request = request,
                        ).bind()
                call.response.header(HttpHeaders.Location, "/api/threads/${dto.id}")
                dto
            }
        }

        get<Threads.ById.Messages> { params ->
            respondEither {
                val user = call.currentUser.bind()
                threadService.listMessages(params.parent.id, user.id, user.roles).bind()
            }
        }

        post<Threads.ById.Messages> { params ->
            respondEither(HttpStatusCode.Created) {
                val user = call.currentUser.bind()
                val request = call.receive<PostMessageRequest>()
                threadService
                    .postMessage(
                        threadIdString = params.parent.id,
                        callerId = user.id,
                        callerName = user.username ?: "Użytkownik",
                        callerRoles = user.roles,
                        request = request,
                    ).bind()
            }
        }

        get<Notifications> { params ->
            respondEither {
                val user = call.currentUser.bind()
                notificationService
                    .list(
                        callerId = user.id,
                        callerRoles = user.roles,
                        unreadOnly = params.unreadOnly,
                        page = params.page,
                        size = params.size,
                    ).bind()
            }
        }

        post<Notifications.Read> { params ->
            respondEither(HttpStatusCode.NoContent) {
                val user = call.currentUser.bind()
                notificationService.markRead(params.id, user.id, user.roles).bind()
            }
        }

        get("/api/notifications/stream") {
            val userEither = call.currentUser
            if (userEither.isLeft()) {
                call.respond(HttpStatusCode.Unauthorized)
                return@get
            }
            val user = (userEither as arrow.core.Either.Right).value
            call.response.cacheControl(CacheControl.NoCache(null))
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                write("retry: 15000\n\n")
                flush()

                eventBus.events
                    .filter { event ->
                        when (event) {
                            is IdeaSubmitted -> {
                                Role.ADMIN in user.roles
                            }

                            is IdeaStatusChanged -> {
                                event.authorId == user.id
                            }

                            is MessageReceived -> {
                                event.recipientIds.contains(user.id) ||
                                    (event.recipientIds.isEmpty() && Role.ADMIN in user.roles)
                            }

                            is CallPublished, is CallChanged -> {
                                true
                            }
                        }
                    }.collect { event ->
                        val eventName = event::class.simpleName ?: "Notification"
                        write("event: $eventName\n")
                        write("data: {\"type\":\"$eventName\",\"occurredAt\":\"${event.occurredAt}\"}\n\n")
                        flush()
                    }
            }
        }
    }
}
