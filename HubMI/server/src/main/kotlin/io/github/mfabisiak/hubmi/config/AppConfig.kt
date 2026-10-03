package io.github.mfabisiak.hubmi.config

/**
 * Server configuration, read from environment variables.
 *
 * [keycloakIssuer] must match the `iss` claim of issued tokens (i.e. the URL clients use to reach Keycloak),
 * while [keycloakJwksUrl] is the URL the server itself uses to download signing keys (inside docker it differs).
 */
data class AppConfig(
    val port: Int,
    val keycloakIssuer: String,
    val keycloakJwksUrl: String,
    val mongoUri: String,
    val mongoDatabase: String,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): AppConfig {
            val issuer = env["KEYCLOAK_ISSUER"] ?: "http://localhost:8081/realms/hubmi"
            return AppConfig(
                port = env["PORT"]?.toInt() ?: 8080,
                keycloakIssuer = issuer,
                keycloakJwksUrl = env["KEYCLOAK_JWKS_URL"] ?: "$issuer/protocol/openid-connect/certs",
                mongoUri = env["MONGO_URI"] ?: "mongodb://localhost:27017/?directConnection=true",
                mongoDatabase = env["MONGO_DATABASE"] ?: "hubmi",
            )
        }
    }
}
