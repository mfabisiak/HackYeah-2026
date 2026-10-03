package io.github.mfabisiak.hubmi.routes

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.model.Filters
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.MatchFeedbackRequest
import io.github.mfabisiak.hubmi.api.MatchRequest
import io.github.mfabisiak.hubmi.api.MatchResult
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.models.NeedItem
import io.github.mfabisiak.hubmi.module
import io.github.mfabisiak.hubmi.repository.needs
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.dsl.module
import kotlin.test.*

class MatchRouteTest {
    private val databaseName = "test-hubmi-matching"
    private val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
    private val alice = TestSecurityHelper.generateToken(userId = "alice", username = "alice")
    private val bob = TestSecurityHelper.generateToken(userId = "bob", username = "bob")

    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = databaseName,
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
        }

    private fun withApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            MongoClient
                .create(
                    MongoTestEnvironment.connectionString,
                ).use { runBlocking { it.getDatabase(databaseName).drop() } }
            application { module(testModule) }
            block(
                createClient {
                    install(Resources)
                    install(ContentNegotiation) { json() }
                },
            )
        }

    private suspend fun storedNeed(needId: String): NeedItem? =
        MongoClient.create(MongoTestEnvironment.connectionString).use { client ->
            client
                .getDatabase(databaseName)
                .needs
                .find(Filters.eq(NeedItem::id, ObjectId(needId)))
                .firstOrNull()
        }

    private suspend fun HttpClient.createInnovation(
        title: String,
        description: String,
        region: String? = null,
    ): InnovationDto =
        post(Innovations()) {
            contentType(ContentType.Application.Json)
            bearerAuth(adminToken)
            setBody(
                UpsertInnovationRequest(
                    title = title,
                    summary = description.take(100),
                    description = description,
                    areas = listOf(SocialArea.AGING),
                    targetGroups = listOf(TargetGroup.SENIORS),
                    stage = InnovationStage.IMPLEMENTED,
                    region = region,
                ),
            )
        }.body()

    private suspend fun HttpClient.match(
        description: String,
        municipality: String? = null,
        token: String? = null,
    ) = post(Matches()) {
        contentType(ContentType.Application.Json)
        token?.let { bearerAuth(it) }
        setBody(MatchRequest(description, municipality))
    }

    private suspend fun HttpClient.answer(
        needId: String,
        helpful: Boolean,
        token: String? = null,
    ) = put(Matches.Feedback(needId = needId)) {
        contentType(ContentType.Application.Json)
        token?.let { bearerAuth(it) }
        setBody(MatchFeedbackRequest(helpful))
    }

    private suspend fun HttpClient.seedCatalogue(): Triple<InnovationDto, InnovationDto, InnovationDto> =
        Triple(
            createInnovation(
                "Sąsiedzka pomoc dla samotnych seniorów",
                "Wolontariusze robią zakupy i odwiedzają samotnych seniorów w domach, żeby przerwać ich izolację.",
                region = "Tarnów",
            ),
            createInnovation(
                "Pomoc psychologiczna dla młodzieży",
                "Rówieśnicy wspierają uczniów w kryzysie psychicznym, lękach i stresie szkolnym.",
            ),
            createInnovation(
                "Mobilna rehabilitacja na wsi",
                "Fizjoterapeuta dojeżdża do chorych na wsi i prowadzi ćwiczenia rehabilitacyjne w domu pacjenta.",
            ),
        )

    @Test
    fun anonymousMatchReturnsRankedInnovationsWithReasons() =
        withApp { client ->
            val (seniors, _, _) = client.seedCatalogue()

            val response = client.match("Samotni seniorzy potrzebują zakupów i odwiedzin wolontariuszy")

            assertEquals(HttpStatusCode.OK, response.status)
            val result = response.body<MatchResult>()
            assertFalse(result.noGoodMatch)
            assertEquals(
                seniors.id,
                result.matches
                    .first()
                    .innovation.id,
            )
            assertTrue(result.matches.first().score in 0.0..1.0)
            assertTrue(
                result.matches
                    .first()
                    .matchedTerms
                    .isNotEmpty(),
            )
            assertTrue(
                result.matches
                    .first()
                    .reasons.size in 1..3,
            )
            assertTrue(ObjectId.isValid(result.needId))
        }

    @Test
    fun municipalityMatchingTheRegionIsExplainedAndBoosted() =
        withApp { client ->
            val (seniors, _, _) = client.seedCatalogue()
            val description = "Samotni seniorzy potrzebują zakupów i odwiedzin wolontariuszy"

            val plain =
                client
                    .match(description)
                    .body<MatchResult>()
                    .matches
                    .first()
            val local =
                client
                    .match(description, municipality = "Tarnów")
                    .body<MatchResult>()
                    .matches
                    .first()

            assertEquals(seniors.id, local.innovation.id)
            assertTrue(local.score > plain.score)
            assertTrue(local.reasons.any { it.startsWith("Region: Tarnów") })
        }

    @Test
    fun unrelatedDescriptionYieldsNoGoodMatch() =
        withApp { client ->
            client.seedCatalogue()

            val result = client.match("Szukam taniego biletu lotniczego do Barcelony").body<MatchResult>()

            assertTrue(result.noGoodMatch)
            assertTrue(result.matches.isEmpty())
            assertNotNull(storedNeed(result.needId), "a need without a match is still stored (it feeds the gap trends)")
        }

    @Test
    fun matchingWorksOnAnEmptyCatalogue() =
        withApp { client ->
            val result = client.match("Samotni seniorzy potrzebują zakupów i odwiedzin").body<MatchResult>()

            assertTrue(result.noGoodMatch)
        }

    @Test
    fun invalidRequestsAreBadRequests() =
        withApp { client ->
            listOf("   ", "ab", "x".repeat(2001)).forEach { description ->
                val response = client.match(description)

                assertEquals(HttpStatusCode.BadRequest, response.status, description.take(10))
                val error = response.body<ErrorResponse>()
                assertEquals(ErrorCode.VALIDATION_FAILED, error.code)
                assertTrue(error.details.any { it.field == "description" })
            }
            val tooLongMunicipality = client.match("Samotni seniorzy potrzebują pomocy", municipality = "x".repeat(101))
            assertEquals(HttpStatusCode.BadRequest, tooLongMunicipality.status)
            assertTrue(tooLongMunicipality.body<ErrorResponse>().details.any { it.field == "municipality" })

            val broken =
                client.post(Matches()) {
                    contentType(ContentType.Application.Json)
                    setBody("{ \"description\": ")
                }
            assertEquals(HttpStatusCode.BadRequest, broken.status)
        }

    @Test
    fun personalDataNeverReachesTheDatabase() =
        withApp { client ->
            client.seedCatalogue()

            val result =
                client
                    .match(
                        "Mój PESEL 44051401359, telefon 501 234 567, jan.kowalski@example.com. Samotni seniorzy potrzebują zakupów",
                        municipality = "Tarnów",
                    ).body<MatchResult>()

            val stored = assertNotNull(storedNeed(result.needId))
            assertFalse("44051401359" in stored.text)
            assertFalse("501 234 567" in stored.text)
            assertFalse("jan.kowalski" in stored.text)
            assertTrue("[PESEL]" in stored.text && "[TELEFON]" in stored.text && "[EMAIL]" in stored.text)
            assertEquals("Tarnów", stored.municipality)
            assertEquals(result.matches.map { ObjectId(it.innovation.id) }, stored.matchedInnovationIds)
            assertEquals(listOf(SocialArea.AGING), stored.areas)
            assertNull(stored.helpful)
        }

    @Test
    fun feedbackIsStoredAndOverwritten() =
        withApp { client ->
            client.seedCatalogue()
            val needId = client.match("Samotni seniorzy potrzebują zakupów").body<MatchResult>().needId

            val first =
                client.put(Matches.Feedback(needId = needId)) {
                    contentType(ContentType.Application.Json)
                    setBody(MatchFeedbackRequest(helpful = true))
                }
            assertEquals(HttpStatusCode.NoContent, first.status)
            assertEquals(true, storedNeed(needId)?.helpful)

            val second =
                client.put(Matches.Feedback(needId = needId)) {
                    contentType(ContentType.Application.Json)
                    setBody(MatchFeedbackRequest(helpful = false))
                }
            assertEquals(HttpStatusCode.NoContent, second.status)
            assertEquals(false, storedNeed(needId)?.helpful)
        }

    @Test
    fun feedbackForUnknownOrMalformedNeedIsRejected() =
        withApp { client ->
            suspend fun feedback(needId: String) =
                client.put(Matches.Feedback(needId = needId)) {
                    contentType(ContentType.Application.Json)
                    setBody(MatchFeedbackRequest(helpful = true))
                }

            assertEquals(HttpStatusCode.NotFound, feedback(ObjectId().toHexString()).status)
            assertEquals(HttpStatusCode.BadRequest, feedback("not-an-id").status)
        }

    @Test
    fun innovationChangesShowUpInMatchingWithoutARestart() =
        withApp { client ->
            val (seniors, _, _) = client.seedCatalogue()
            val query = "Samotni seniorzy potrzebują zakupów i odwiedzin wolontariuszy"
            assertEquals(
                seniors.id,
                client
                    .match(query)
                    .body<MatchResult>()
                    .matches
                    .first()
                    .innovation.id,
            )

            val newer =
                client.createInnovation(
                    "Zakupy i odwiedziny dla samotnych seniorów",
                    "Wolontariusze robią zakupy, odwiedzają samotnych seniorów i dotrzymują im towarzystwa.",
                )
            val afterCreate =
                client
                    .match(query)
                    .body<MatchResult>()
                    .matches
                    .map { it.innovation.id }
            assertTrue(newer.id in afterCreate, "a new innovation should be matchable immediately")

            client.delete(Innovations.ById(id = newer.id)) { bearerAuth(adminToken) }
            client.delete(Innovations.ById(id = seniors.id)) { bearerAuth(adminToken) }
            val afterDelete = client.match(query).body<MatchResult>()
            assertTrue(afterDelete.matches.none { it.innovation.id == newer.id || it.innovation.id == seniors.id })
        }

    @Test
    fun similarNeedsAreOtherNeedsMatchedToTheSameInnovation() =
        withApp { client ->
            client.seedCatalogue()
            val first =
                client
                    .match(
                        "Samotni seniorzy potrzebują zakupów i odwiedzin wolontariuszy",
                    ).body<MatchResult>()
            assertTrue(first.similarNeeds.isEmpty())

            val second = client.match("Seniorzy samotni, brakuje zakupów i odwiedzin wolontariuszy").body<MatchResult>()

            assertEquals(listOf(first.needId), second.similarNeeds.map { it.id })
            assertTrue(
                second.similarNeeds
                    .single()
                    .excerpt
                    .startsWith("Samotni seniorzy"),
            )
            assertEquals(SocialArea.AGING, second.similarNeeds.single().area)
            assertTrue(
                second.similarNeeds
                    .single()
                    .excerpt.length <= 121,
            )
        }

    @Test
    fun problemDiagnosisOfTheApplicationFormIsMatchable() =
        withApp { client ->
            client.seedCatalogue()
            val created =
                client
                    .post(Innovations()) {
                        contentType(ContentType.Application.Json)
                        bearerAuth(adminToken)
                        setBody(
                            UpsertInnovationRequest(
                                title = "Program Bliżej",
                                summary = "Nowa usługa lokalna.",
                                description = "Usługa świadczona w małych miejscowościach.",
                                areas = listOf(SocialArea.SERVICE_ACCESS),
                                targetGroups = listOf(TargetGroup.RESIDENTS),
                                stage = InnovationStage.PILOT,
                                problemDiagnosis =
                                    "Mieszkańcy odległych sołectw nie mają autobusu, brakuje dojazdu do ośrodka zdrowia i urzędu.",
                            ),
                        )
                    }.body<InnovationDto>()

            val result =
                client
                    .match(
                        "Z naszego sołectwa brakuje dojazdu do ośrodka zdrowia i nie ma autobusu",
                    ).body<MatchResult>()

            assertEquals(
                created.id,
                result.matches
                    .firstOrNull()
                    ?.innovation
                    ?.id,
            )
        }

    @Test
    fun loggedInReporterOwnsTheNeedAndAnonymousOneHasNoOwner() =
        withApp { client ->
            client.seedCatalogue()
            val query = "Samotni seniorzy potrzebują zakupów i odwiedzin wolontariuszy"

            val owned = client.match(query, token = alice).body<MatchResult>()
            val anonymous = client.match(query).body<MatchResult>()

            assertEquals("alice", storedNeed(owned.needId)?.ownerId)
            assertNull(storedNeed(anonymous.needId)?.ownerId)
        }

    @Test
    fun onlyTheOwnerMayAnswerForAnOwnedNeed() =
        withApp { client ->
            client.seedCatalogue()
            val needId = client.match("Samotni seniorzy potrzebują zakupów", token = alice).body<MatchResult>().needId

            assertEquals(HttpStatusCode.Unauthorized, client.answer(needId, true).status)
            assertEquals(HttpStatusCode.Forbidden, client.answer(needId, true, bob).status)
            assertNull(storedNeed(needId)?.helpful, "rejected answers must not be stored")

            assertEquals(HttpStatusCode.NoContent, client.answer(needId, true, alice).status)
            assertEquals(true, storedNeed(needId)?.helpful)
        }

    @Test
    fun anonymousNeedCanBeAnsweredWithItsIdAlone() =
        withApp { client ->
            client.seedCatalogue()
            val needId = client.match("Samotni seniorzy potrzebują zakupów").body<MatchResult>().needId

            assertEquals(HttpStatusCode.NoContent, client.answer(needId, true).status)
            assertEquals(HttpStatusCode.NoContent, client.answer(needId, false, bob).status)
            assertEquals(false, storedNeed(needId)?.helpful)
        }

    @Test
    fun anInvalidTokenIsNotTreatedAsAnonymous() =
        withApp { client ->
            client.seedCatalogue()

            val response = client.match("Samotni seniorzy potrzebują zakupów", token = "not-a-jwt")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
