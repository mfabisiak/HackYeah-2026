package io.github.mfabisiak.hubmi.routes

import io.github.mfabisiak.hubmi.api.AdminIdeas
import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.Challenges
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.Ideas
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.api.Materials
import io.github.mfabisiak.hubmi.api.Notifications
import io.github.mfabisiak.hubmi.api.Threads
import io.github.mfabisiak.hubmi.plugins.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.plugins.requireRole
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.*
import io.ktor.server.routing.*

private suspend fun RoutingContext.notImplemented() =
    call.respond(HttpStatusCode.NotImplemented, ErrorResponse("not_implemented", "Not implemented yet"))

/**
 * Placeholders for the planned API (see docs/API.md). They do nothing yet and answer `501`, but already enforce the
 * intended access policy. Replace a stub with the real route when its module is implemented.
 */
fun Route.contractStubs() {
    publicStubs()
    authenticate(KEYCLOAK_AUTH) {
        authenticatedStubs()
        requireRole("admin") { adminStubs() }
    }
}

private fun Route.publicStubs() {
    get<Innovations> { notImplemented() }
    get<Innovations.ById> { notImplemented() }
    get<Challenges> { notImplemented() }
    get<Challenges.ById> { notImplemented() }
    get<Materials> { notImplemented() }
    get<Materials.ById> { notImplemented() }
    get<Calls> { notImplemented() }
    get<Calls.Active> { notImplemented() }
    post<Matches> { notImplemented() }
    post<Matches.Feedback> { notImplemented() }
}

private fun Route.authenticatedStubs() {
    post<Innovations.ById.TestRequests> { notImplemented() }
    post<Innovations.ById.Feedback> { notImplemented() }
    post<Ideas> { notImplemented() }
    get<Ideas.Mine> { notImplemented() }
    get<Ideas.ById> { notImplemented() }
    post<Calls.ById.Applications> { notImplemented() }
    get<Threads> { notImplemented() }
    post<Threads> { notImplemented() }
    get<Threads.ById.Messages> { notImplemented() }
    post<Threads.ById.Messages> { notImplemented() }
    get<Notifications> { notImplemented() }
    post<Notifications.Read> { notImplemented() }
}

private fun Route.adminStubs() {
    post<Innovations> { notImplemented() }
    put<Innovations.ById> { notImplemented() }
    delete<Innovations.ById> { notImplemented() }
    post<Challenges> { notImplemented() }
    put<Challenges.ById> { notImplemented() }
    delete<Challenges.ById> { notImplemented() }
    post<Materials> { notImplemented() }
    put<Materials.ById> { notImplemented() }
    delete<Materials.ById> { notImplemented() }
    post<Calls> { notImplemented() }
    put<Calls.ById> { notImplemented() }
    delete<Calls.ById> { notImplemented() }
    get<AdminTrends> { notImplemented() }
    get<AdminIdeas> { notImplemented() }
    patch<AdminIdeas.Status> { notImplemented() }
}
