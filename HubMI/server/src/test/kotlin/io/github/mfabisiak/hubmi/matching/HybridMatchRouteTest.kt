package io.github.mfabisiak.hubmi.matching

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.MatchRequest
import io.github.mfabisiak.hubmi.api.MatchResult
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.config.MatchingMode
import io.github.mfabisiak.hubmi.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `MATCHING_MODE=hybrid` wired through Koin and HTTP, with a fake model or none at all. */
class HybridMatchRouteTest {
    private val databaseName = "test-hubmi-matching-hybrid"
    private val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))

    private fun config(ollamaUrl: String = "http://localhost:1") =
        module {
            single {
                AppConfig(
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = databaseName,
                    matchingMode = MatchingMode.HYBRID,
                    ollamaUrl = ollamaUrl,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun withApp(
        vararg extra: Module,
        block: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
    ) = testApplication {
        MongoClient
            .create(MongoTestEnvironment.connectionString)
            .use { runBlocking { it.getDatabase(databaseName).drop() } }
        application { module(config(), *extra) }
        block(
            createClient {
                install(Resources)
                install(ContentNegotiation) { json() }
            },
        )
    }

    private suspend fun HttpClient.createInnovation(
        title: String,
        summary: String,
    ): InnovationDto =
        post(Innovations()) {
            contentType(ContentType.Application.Json)
            bearerAuth(adminToken)
            setBody(
                UpsertInnovationRequest(
                    title = title,
                    summary = summary,
                    description = summary,
                    areas = listOf(SocialArea.AGING),
                    targetGroups = listOf(TargetGroup.SENIORS),
                    stage = InnovationStage.IMPLEMENTED,
                ),
            )
        }.body()

    private suspend fun HttpClient.match(description: String) =
        post(Matches()) {
            contentType(ContentType.Application.Json)
            setBody(MatchRequest(description, null))
        }

    @Test
    fun withoutOllamaMatchingFallsBackToKeywordsInsteadOfFailing() =
        withApp { client ->
            val seniors =
                client.createInnovation("Sąsiedzka pomoc dla seniorów", "Wolontariusze robią zakupy samotnym seniorom")
            client.createInnovation("Ogrody miejskie", "Wspólna uprawa warzyw")

            val response = client.match("Samotni seniorzy potrzebują zakupów")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(
                seniors.id,
                response
                    .body<MatchResult>()
                    .matches
                    .first()
                    .innovation.id,
            )
        }

    @Test
    fun withAModelAnInnovationWithoutAnySharedWordIsFound() {
        val embedder = HybridTestFixtures.embedder()
        withApp(module { single<EmbeddingClient> { embedder } }) { client ->
            val transport = client.createInnovation("Transport door-to-door", "Dowóz osób na wózkach do przychodni")
            client.createInnovation("Ogrody miejskie", "Wspólna uprawa warzyw")

            val result = client.match("Brak jak dojechać do lekarza").body<MatchResult>()

            assertFalse(result.noGoodMatch)
            assertEquals(
                transport.id,
                result.matches
                    .first()
                    .innovation.id,
            )
            assertTrue("Zbliżony znaczeniowo do opisu problemu." in result.matches.first().reasons)
        }
    }
}
