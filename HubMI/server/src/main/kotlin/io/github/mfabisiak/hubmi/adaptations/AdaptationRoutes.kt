package io.github.mfabisiak.hubmi.adaptations

import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.AdaptationEvent
import io.github.mfabisiak.hubmi.api.Adaptations
import io.github.mfabisiak.hubmi.api.AdminAdaptations
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.UpdateAdaptationStatusRequest
import io.github.mfabisiak.hubmi.api.isStaff
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.auth.currentUser
import io.github.mfabisiak.hubmi.auth.requireStaff
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.common.http.respondError
import io.github.mfabisiak.hubmi.common.http.respondEventStream
import io.github.mfabisiak.hubmi.common.http.wantsEventStream
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import org.koin.ktor.ext.inject

private const val INVALID_INNOVATION_ID = "Nieprawidłowy identyfikator innowacji"
private const val INVALID_ADAPTATION_ID = "Nieprawidłowy identyfikator planu adaptacji"
private const val INVALID_INSTITUTION = "Błąd walidacji opisu instytucji"
private const val INVALID_COMMENT = "Błąd walidacji komentarza"

/**
 * The Middleman. Any logged-in user can ask for a plan; it answers as JSON (`201` with the stored plan, `200` when
 * the model produced none) or, for `Accept: text/event-stream`, as a stream of the plan's steps ending with the stored
 * plan. Authors read their own plans, admins read and review all of them.
 */
fun Route.adaptationRoutes() {
    val service by inject<AdaptationService>()

    authenticate(KEYCLOAK_AUTH) {
        post<Innovations.ById.Adaptations> { resource ->
            val request = call.receive<InstitutionProfile>()
            val answer =
                either {
                    val user = call.currentUser.bind()
                    val innovationId =
                        InnovationId.parse(resource.parent.id).orValidationError(INVALID_INNOVATION_ID).bind()
                    val draft = InstitutionDraft.parse(request).orValidationError(INVALID_INSTITUTION).bind()
                    service.adapt(innovationId, UserId(user.id), draft).bind()
                }
            answer.fold(
                ifLeft = { respondError(it) },
                ifRight = { respondAdaptation(it) },
            )
        }

        get<Adaptations.Mine> { resource ->
            respondEither {
                val user = call.currentUser.bind()
                val pageRequest = validatePageRequest(resource.page, resource.size).bind()
                service.mine(UserId(user.id), pageRequest).bind()
            }
        }

        get<Adaptations.ById> { resource ->
            respondEither {
                val user = call.currentUser.bind()
                val id = AdaptationId.parse(resource.id).orValidationError(INVALID_ADAPTATION_ID).bind()
                service.get(id, UserId(user.id), callerIsStaff = user.roles.isStaff()).bind()
            }
        }

        requireStaff {
            get<AdminAdaptations> { resource ->
                respondEither {
                    val pageRequest = validatePageRequest(resource.page, resource.size).bind()
                    service.list(resource.status, pageRequest).bind()
                }
            }

            patch<AdminAdaptations.Status> { resource ->
                val request = call.receive<UpdateAdaptationStatusRequest>()
                respondEither {
                    val id = AdaptationId.parse(resource.id).orValidationError(INVALID_ADAPTATION_ID).bind()
                    val comment =
                        request.comment
                            ?.takeIf(String::isNotBlank)
                            ?.let {
                                ReviewComment
                                    .parse(it)
                                    .asNel()
                                    .orValidationError(INVALID_COMMENT)
                                    .bind()
                            }
                    service.review(id, request.status, comment).bind()
                }
            }
        }
    }
}

private suspend fun RoutingContext.respondAdaptation(events: Flow<AdaptationEvent>) {
    if (call.wantsEventStream()) {
        respondEventStream(events, AdaptationEvent.serializer(), AdaptationEvent::eventName)
    } else {
        when (val closing = events.last()) {
            is AdaptationEvent.Done -> {
                val adaptation = closing.response.adaptation
                adaptation?.let { call.response.header(HttpHeaders.Location, "/api/adaptations/${it.id}") }
                call.respond(if (adaptation == null) HttpStatusCode.OK else HttpStatusCode.Created, closing.response)
            }

            is AdaptationEvent.Failed -> {
                call.respond(HttpStatusCode.InternalServerError, closing.error)
            }

            is AdaptationEvent.Step -> {
                respondError(DomainError.Internal())
            }
        }
    }
}

private fun AdaptationEvent.eventName(): String =
    when (this) {
        is AdaptationEvent.Step -> AdaptationEvent.STEP
        is AdaptationEvent.Done -> AdaptationEvent.DONE
        is AdaptationEvent.Failed -> AdaptationEvent.FAILED
    }
