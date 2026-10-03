package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*

private suspend fun RoutingContext.notImplemented() =
    call.respond(HttpStatusCode.NotImplemented, ErrorResponse(ErrorCode.NOT_IMPLEMENTED, "Not implemented yet"))

/**
 * Placeholders for the planned API (see docs/API.md). They do nothing yet and answer `501`, but already enforce the
 * intended access policy. Replace a stub with the real route when its module is implemented.
 */
fun Route.contractStubs() {
    publicStubs()
    authenticate(KEYCLOAK_AUTH) {
        authenticatedStubs()
        requireRole(Role.ADMIN) { adminStubs() }
    }
}

private fun Route.publicStubs() {
    post<Matches> { notImplemented() }
    post<Matches.Feedback> { notImplemented() }
}

private fun Route.authenticatedStubs() {
    post<Innovations.ById.TestRequests> { notImplemented() }
    post<Innovations.ById.Feedback> { notImplemented() }
    get<Threads> { notImplemented() }
    post<Threads> { notImplemented() }
    get<Threads.ById.Messages> { notImplemented() }
    post<Threads.ById.Messages> { notImplemented() }
    get<Notifications> { notImplemented() }
    post<Notifications.Read> { notImplemented() }
}

private fun Route.adminStubs() {
    get<AdminTrends> { notImplemented() }
}
