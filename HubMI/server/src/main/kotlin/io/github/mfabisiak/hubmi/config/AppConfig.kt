package io.github.mfabisiak.hubmi.config

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import java.net.URI

sealed interface ConfigError {
    data class InvalidNumber(
        val key: String,
        val value: String,
    ) : ConfigError

    data class InvalidUrl(
        val key: String,
        val value: String,
    ) : ConfigError
}

/**
 * Server configuration. Defaults suit local development; [fromEnv] overlays environment variables.
 *
 * [keycloakIssuer] must match the `iss` claim of issued tokens (i.e. the URL clients use to reach Keycloak),
 * while [keycloakJwksUrl] is the URL the server itself uses to download signing keys (inside docker it differs).
 */
data class AppConfig(
    val port: Int = 8080,
    val keycloakIssuer: String = "http://localhost:8081/realms/hubmi",
    val keycloakJwksUrl: String = "$keycloakIssuer/protocol/openid-connect/certs",
    val mongoUri: String = "mongodb://localhost:27017/?directConnection=true",
    val mongoDatabase: String = "hubmi",
    val seed: Boolean = false,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Either<ConfigError, AppConfig> =
            either {
                val defaults = AppConfig()
                val issuer = env["KEYCLOAK_ISSUER"] ?: defaults.keycloakIssuer
                val jwksUrl = env["KEYCLOAK_JWKS_URL"] ?: "$issuer/protocol/openid-connect/certs"
                ensureNotNull(Either.catch { URI(jwksUrl).toURL() }.getOrNull()) {
                    ConfigError.InvalidUrl("KEYCLOAK_JWKS_URL", jwksUrl)
                }
                val port =
                    env["PORT"]?.let { raw ->
                        ensureNotNull(raw.toIntOrNull()) { ConfigError.InvalidNumber("PORT", raw) }
                    } ?: defaults.port
                AppConfig(
                    port = port,
                    keycloakIssuer = issuer,
                    keycloakJwksUrl = jwksUrl,
                    mongoUri = env["MONGO_URI"] ?: defaults.mongoUri,
                    mongoDatabase = env["MONGO_DATABASE"] ?: defaults.mongoDatabase,
                    seed = env["SEED"]?.toBooleanStrictOrNull() ?: defaults.seed,
                )
            }
    }
}
