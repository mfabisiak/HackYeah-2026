package io.github.mfabisiak.hubmi.innovations

import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.requireRole
import io.github.mfabisiak.hubmi.common.SearchQuery
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.common.http.respondEitherUnit
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

private const val INVALID_ID = "Nieprawidłowy identyfikator innowacji"
private const val INVALID_INNOVATION = "Błąd walidacji danych innowacji"

fun Route.innovationRoutes() {
    val service by inject<InnovationService>()

    // Public
    get<Innovations> { resource ->
        respondEither {
            val pageRequest = validatePageRequest(resource.page, resource.size).bind()
            val q = SearchQuery.parse(resource.q).orValidationError("Nieprawidłowe zapytanie").bind()
            service.list(q, resource.area, resource.targetGroup, pageRequest).bind()
        }
    }

    get<Innovations.ById> { resource ->
        respondEither {
            val id = InnovationId.parse(resource.id).orValidationError(INVALID_ID).bind()
            service.getById(id).bind()
        }
    }

    // Admin only
    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
            post<Innovations> {
                val request = call.receive<UpsertInnovationRequest>()
                respondEither(successStatus = HttpStatusCode.Created) {
                    val draft = InnovationDraft.parse(request).orValidationError(INVALID_INNOVATION).bind()
                    service.create(draft).bind()
                }
            }

            put<Innovations.ById> { resource ->
                val request = call.receive<UpsertInnovationRequest>()
                respondEither {
                    val id = InnovationId.parse(resource.id).orValidationError(INVALID_ID).bind()
                    val draft = InnovationDraft.parse(request).orValidationError(INVALID_INNOVATION).bind()
                    service.update(id, draft).bind()
                }
            }

            delete<Innovations.ById> { resource ->
                respondEitherUnit {
                    val id = InnovationId.parse(resource.id).orValidationError(INVALID_ID).bind()
                    service.delete(id).bind()
                }
            }
        }
    }
}
