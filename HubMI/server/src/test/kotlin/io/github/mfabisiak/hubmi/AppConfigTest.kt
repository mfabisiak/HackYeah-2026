package io.github.mfabisiak.hubmi

import arrow.core.Either
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.config.ConfigError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppConfigTest {
    @Test
    fun emptyEnvironmentGivesDefaults() {
        assertEquals(Either.Right(AppConfig()), AppConfig.fromEnv(emptyMap()))
    }

    @Test
    fun jwksUrlIsDerivedFromIssuerUnlessOverridden() {
        val derived = AppConfig.fromEnv(mapOf("KEYCLOAK_ISSUER" to "http://kc/realms/x"))
        assertEquals("http://kc/realms/x/protocol/openid-connect/certs", derived.getOrNull()?.keycloakJwksUrl)

        val overridden =
            AppConfig.fromEnv(
                mapOf("KEYCLOAK_ISSUER" to "http://kc/realms/x", "KEYCLOAK_JWKS_URL" to "http://internal/certs"),
            )
        assertEquals("http://internal/certs", overridden.getOrNull()?.keycloakJwksUrl)
    }

    @Test
    fun invalidPortIsAnError() {
        val result = AppConfig.fromEnv(mapOf("PORT" to "eighty"))
        assertEquals(ConfigError.InvalidNumber("PORT", "eighty"), result.leftOrNull())
    }

    @Test
    fun invalidJwksUrlIsAnError() {
        val result = AppConfig.fromEnv(mapOf("KEYCLOAK_JWKS_URL" to "not a url"))
        assertIs<ConfigError.InvalidUrl>(result.leftOrNull())
    }
}
