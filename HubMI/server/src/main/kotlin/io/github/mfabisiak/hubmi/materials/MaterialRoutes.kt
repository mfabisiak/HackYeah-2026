package io.github.mfabisiak.hubmi.materials

import io.github.mfabisiak.hubmi.api.Materials
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
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

private const val INVALID_MATERIAL_ID = "Nieprawidłowy identyfikator materiału"
private const val INVALID_MATERIAL = "Błąd walidacji danych materiału"

fun Route.materialRoutes() {
    val materialService by inject<MaterialService>()

    get<Materials> { resource ->
        respondEither {
            val pageRequest = validatePageRequest(resource.page, resource.size).bind()
            val q = SearchQuery.parse(resource.q).orValidationError("Nieprawidłowe zapytanie").bind()
            materialService.list(q, resource.area, resource.type, pageRequest).bind()
        }
    }

    get<Materials.ById> { resource ->
        respondEither {
            val id = MaterialId.parse(resource.id).orValidationError(INVALID_MATERIAL_ID).bind()
            materialService.getById(id).bind()
        }
    }

    // Admin only
    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
            post<Materials> {
                val request = call.receive<UpsertMaterialRequest>()
                respondEither(successStatus = HttpStatusCode.Created) {
                    val draft = MaterialDraft.parse(request).orValidationError(INVALID_MATERIAL).bind()
                    materialService.create(draft).bind()
                }
            }

            put<Materials.ById> { resource ->
                val request = call.receive<UpsertMaterialRequest>()
                respondEither {
                    val id = MaterialId.parse(resource.id).orValidationError(INVALID_MATERIAL_ID).bind()
                    val draft = MaterialDraft.parse(request).orValidationError(INVALID_MATERIAL).bind()
                    materialService.update(id, draft).bind()
                }
            }

            delete<Materials.ById> { resource ->
                respondEitherUnit {
                    val id = MaterialId.parse(resource.id).orValidationError(INVALID_MATERIAL_ID).bind()
                    materialService.delete(id).bind()
                }
            }
        }
    }
}
