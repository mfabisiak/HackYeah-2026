package io.github.mfabisiak.hubmi.auth

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.auth0.jwk.JwkProvider
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.config.AppConfig
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

/** Authenticated user extracted from JWT claims (`id = sub`, `username`, `name`, `roles`). */
data class CurrentUser(
    val id: String,
    val username: String?,
    val roles: Set<Role>,
    val email: String? = null,
    val name: String? = null,
) {
    /** How the user is shown to others, e.g. as the author of a message: full name, else username. */
    val displayName: String get() = name ?: username ?: ANONYMOUS_NAME
}

private const val ANONYMOUS_NAME = "Użytkownik"

/**
 * Realm roles assigned in Keycloak (`realm_access.roles` claim); the JWT library exposes claims untyped.
 * Roles unknown to the platform (e.g. Keycloak's built-in `offline_access`) are dropped.
 */
val JWTPrincipal.realmRoles: Set<Role>
    get() =
        payload
            .getClaim("realm_access")
            .asMap()
            ?.get("roles")
            ?.let { it as? Collection<*> }
            ?.filterIsInstance<String>()
            ?.mapNotNull(Role::fromKeycloakName)
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
            name =
                principal.payload
                    .getClaim("name")
                    .asString()
                    ?.takeIf(String::isNotBlank),
            roles = principal.realmRoles,
        ).right()
    }

/** Allows the request only if the authenticated user has at least one of [roles]. */
fun Route.requireAnyRole(
    vararg roles: Role,
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
        createRouteScopedPlugin("RequireAnyRole-${roles.joinToString("-") { it.name }}") {
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

/** Allows the request only for ROPS officials ([Role.staff]); use inside an `authenticate { }` block. */
fun Route.requireStaff(build: Route.() -> Unit): Route = requireAnyRole(*Role.staff.toTypedArray(), build = build)

/** Allows the request only if the authenticated user has [role]; use inside an `authenticate { }` block. */
fun Route.requireRole(
    role: Role,
    build: Route.() -> Unit,
): Route = requireAnyRole(role, build = build)
