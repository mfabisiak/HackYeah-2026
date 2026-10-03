package io.github.mfabisiak.hubmi.plugins

import com.auth0.jwk.JwkProviderBuilder
import io.github.mfabisiak.hubmi.config.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.net.URI
import java.util.concurrent.TimeUnit

const val KEYCLOAK_AUTH = "keycloak"

fun Application.configureSecurity() {
    val config by inject<AppConfig>()

    // The JWKS URL is validated when the configuration is loaded. Signing keys are fetched lazily from Keycloak on the first token, so the server can start before Keycloak.
    val jwkProvider =
        JwkProviderBuilder(URI(config.keycloakJwksUrl).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

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

/** Allows the request only if the authenticated user has [role]; use inside an `authenticate { }` block. */
fun Route.requireRole(
    role: String,
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
        createRouteScopedPlugin("RequireRole-$role") {
            on(AuthenticationChecked) { call ->
                val principal = call.principal<JWTPrincipal>()
                if (principal == null || role !in principal.realmRoles) {
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        },
    )
    route.build()
    return route
}
