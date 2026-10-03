package io.github.mfabisiak.hubmi.plugins

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.auth0.jwk.JwkProvider
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.service.DomainError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

const val KEYCLOAK_AUTH = "keycloak"

fun Application.configureSecurity() {
    val config by inject<AppConfig>()
    val jwkProvider by inject<JwkProvider>()

    install(Authentication) {
        jwt(KEYCLOAK_AUTH) {
            realm = "hubmi"
            verifier(jwkProvider, config.keycloakIssuer) {
                acceptLeeway(3)
            }
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}

/** Authenticated user extracted from JWT claims (`id = sub`, `username`, `roles`). */
data class CurrentUser(
    val id: String,
    val username: String?,
    val roles: Set<String>,
    val email: String? = null,
) {
    val isAdmin: Boolean get() = "admin" in roles
    val isExpert: Boolean get() = "expert" in roles
    val isUser: Boolean get() = "user" in roles

    fun hasRole(role: String): Boolean = role in roles

    /** Helper: checks whether this user is the resource owner OR has the specified role. */
    fun isOwnerOrHasRole(
        ownerId: String,
        role: String,
    ): Boolean = id == ownerId || role in roles

    fun isOwnerOrAdmin(ownerId: String): Boolean = isOwnerOrHasRole(ownerId, "admin")
}

typealias UserContext = CurrentUser

/** Realm roles assigned in Keycloak (`realm_access.roles` claim); the JWT library exposes claims untyped. */
val JWTPrincipal.realmRoles: Set<String>
    get() =
        payload
            .getClaim("realm_access")
            .asMap()
            ?.get("roles")
            ?.let { it as? Collection<*> }
            ?.filterIsInstance<String>()
            ?.toSet()
            .orEmpty()

/** Extracts the authenticated [CurrentUser] safely, returning [DomainError.Unauthorized] if missing. */
val ApplicationCall.currentUser: Either<DomainError.Unauthorized, CurrentUser>
    get() {
        val principal = principal<JWTPrincipal>() ?: return DomainError.Unauthorized().left()
        val subject = principal.subject ?: return DomainError.Unauthorized().left()
        return CurrentUser(
            id = subject,
            username = principal.payload.getClaim("preferred_username").asString(),
            email = principal.payload.getClaim("email").asString(),
            roles = principal.realmRoles,
        ).right()
    }

val ApplicationCall.userContext: Either<DomainError.Unauthorized, CurrentUser>
    get() = currentUser

/** Allows the request only if the authenticated user has at least one of [roles]. */
fun Route.requireAnyRole(
    vararg roles: String,
    build: Route.() -> Unit,
): Route {
    val route =
        createChild(
            object : RouteSelector() {
                override suspend fun evaluate(
                    context: RoutingResolveContext,
                    segmentIndex: Int,
                ) = RouteSelectorEvaluation.Transparent
            },
        )
    route.install(
        createRouteScopedPlugin("RequireAnyRole-${roles.joinToString("-")}") {
            on(AuthenticationChecked) { call ->
                val principal = call.principal<JWTPrincipal>()
                if (principal == null || roles.none { it in principal.realmRoles }) {
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        },
    )
    route.build()
    return route
}

/** Allows the request only if the authenticated user has [role]; use inside an `authenticate { }` block. */
fun Route.requireRole(
    role: String,
    build: Route.() -> Unit,
): Route = requireAnyRole(role, build = build)
