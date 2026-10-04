package io.github.mfabisiak.hubmi

import arrow.core.Either
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.config.ConfigError
import io.github.mfabisiak.hubmi.config.MatchingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppConfigTest {
    @Test
    fun emptyEnvironmentGivesDefaults() {
        assertEquals(
            Either.Right(AppConfig(matchingMode = MatchingMode.HYBRID, assistantEnabled = true)),
            AppConfig.fromEnv(emptyMap()),
        )
    }

    @Test
    fun matchingModeIsReadCaseInsensitively() {
        assertEquals(
            MatchingMode.KEYWORD,
            AppConfig.fromEnv(mapOf("MATCHING_MODE" to "Keyword")).getOrNull()?.matchingMode,
        )
        assertEquals(
            MatchingMode.HYBRID,
            AppConfig.fromEnv(mapOf("MATCHING_MODE" to "hybrid")).getOrNull()?.matchingMode,
        )
    }

    @Test
    fun unknownMatchingModeIsAnError() {
        val result = AppConfig.fromEnv(mapOf("MATCHING_MODE" to "hybrid+llm"))
        assertEquals(ConfigError.InvalidMatchingMode("MATCHING_MODE", "hybrid+llm"), result.leftOrNull())
    }

    @Test
    fun ollamaSettingsComeFromTheEnvironment() {
        val config =
            AppConfig.fromEnv(mapOf("OLLAMA_URL" to "http://host.docker.internal:11434", "EMBEDDING_MODEL" to "x"))
        assertEquals("http://host.docker.internal:11434", config.getOrNull()?.ollamaUrl)
        assertEquals("x", config.getOrNull()?.embeddingModel)
    }

    @Test
    fun theAssistantIsOnUnlessSwitchedOffAndUsesTheConfiguredModel() {
        assertEquals(false, AppConfig.fromEnv(mapOf("ASSISTANT_ENABLED" to "false")).getOrNull()?.assistantEnabled)
        assertEquals("m", AppConfig.fromEnv(mapOf("LLM_MODEL" to "m")).getOrNull()?.llmModel)
        assertEquals(
            ConfigError.InvalidBoolean("ASSISTANT_ENABLED", "maybe"),
            AppConfig.fromEnv(mapOf("ASSISTANT_ENABLED" to "maybe")).leftOrNull(),
        )
    }

    @Test
    fun invalidOllamaUrlIsAnError() {
        assertIs<ConfigError.InvalidUrl>(AppConfig.fromEnv(mapOf("OLLAMA_URL" to "not a url")).leftOrNull())
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
