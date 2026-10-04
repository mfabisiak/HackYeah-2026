package io.github.mfabisiak.hubmi.admin

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AdminSummary
import io.github.mfabisiak.hubmi.api.AdminSummaryDto
import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.TrendsDto
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.ideas.IdeaItem
import io.github.mfabisiak.hubmi.ideas.ideas
import io.github.mfabisiak.hubmi.matching.NeedItem
import io.github.mfabisiak.hubmi.matching.needs
import io.github.mfabisiak.hubmi.messaging.ThreadItem
import io.github.mfabisiak.hubmi.messaging.threads
import io.github.mfabisiak.hubmi.module
import io.github.mfabisiak.hubmi.tester.TestRequestItem
import io.github.mfabisiak.hubmi.tester.testRequests
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.dsl.module
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.*

class AdminRouteTest {
    private val databaseName = "test-hubmi-admin"
    private val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
    private val userToken = TestSecurityHelper.generateToken(roles = setOf(Role.USER))
    private val expertToken = TestSecurityHelper.generateToken(roles = setOf(Role.EXPERT))

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
            MongoClient.create(MongoTestEnvironment.connectionString).use {
                runBlocking { it.getDatabase(databaseName).drop() }
            }
            application { module(testModule) }
            block(
                createClient {
                    install(Resources)
                    install(ContentNegotiation) { json() }
                },
            )
        }

    @Test
    fun unauthenticatedRequestsGet401() =
        withApp { client ->
            val trendsResp = client.get(AdminTrends())
            assertEquals(HttpStatusCode.Unauthorized, trendsResp.status)

            val summaryResp = client.get(AdminSummary())
            assertEquals(HttpStatusCode.Unauthorized, summaryResp.status)
        }

    @Test
    fun nonAdminRolesGet403() =
        withApp { client ->
            // Role.USER
            val userTrends =
                client.get(AdminTrends()) {
                    bearerAuth(userToken)
                }
            assertEquals(HttpStatusCode.Forbidden, userTrends.status)

            val userSummary =
                client.get(AdminSummary()) {
                    bearerAuth(userToken)
                }
            assertEquals(HttpStatusCode.Forbidden, userSummary.status)

            // Role.EXPERT
            val expertTrends =
                client.get(AdminTrends()) {
                    bearerAuth(expertToken)
                }
            assertEquals(HttpStatusCode.Forbidden, expertTrends.status)
        }

    @Test
    fun adminCanGetTrendsWithControlledNeedsAndPrivacyThreshold() =
        withApp { client ->
            val now = Instant.now()
            val mongoClient = MongoClient.create(MongoTestEnvironment.connectionString)
            val db = mongoClient.getDatabase(databaseName)

            // Current window needs (within last 6 months)
            // Kraków: 3 items (>= 3 threshold)
            val need1 =
                NeedItem(
                    text = "Seniorzy szukają transportu w samotności",
                    municipality = "Kraków",
                    areas = listOf(SocialArea.LONELINESS),
                    matchedInnovationIds = listOf(ObjectId()),
                    noGoodMatch = false,
                    createdAt = now.minus(10, ChronoUnit.DAYS).toString(),
                )
            val need2 =
                NeedItem(
                    text = "Spotkania dla osób starszych w samotności",
                    municipality = "Kraków",
                    areas = listOf(SocialArea.LONELINESS),
                    matchedInnovationIds = listOf(ObjectId()),
                    noGoodMatch = false,
                    createdAt = now.minus(20, ChronoUnit.DAYS).toString(),
                )
            val need3 =
                NeedItem(
                    text = "Klub sąsiedzki przeciwko samotności",
                    municipality = "Kraków",
                    areas = listOf(SocialArea.LONELINESS),
                    matchedInnovationIds = listOf(ObjectId()),
                    noGoodMatch = false,
                    createdAt = now.minus(30, ChronoUnit.DAYS).toString(),
                )
            // Wieliczka: 2 items (< 3 threshold, must be filtered out for privacy)
            val need4 =
                NeedItem(
                    text = "Potrzebny tani transport dla seniorów na wsi",
                    municipality = "Wieliczka",
                    areas = listOf(SocialArea.AGING),
                    matchedInnovationIds = emptyList(),
                    noGoodMatch = true,
                    createdAt = now.minus(40, ChronoUnit.DAYS).toString(),
                )
            val need5 =
                NeedItem(
                    text = "Szybki transport medyczny dla niepełnosprawnych",
                    municipality = "Wieliczka",
                    areas = listOf(SocialArea.AGING),
                    matchedInnovationIds = emptyList(),
                    noGoodMatch = true,
                    createdAt = now.minus(50, ChronoUnit.DAYS).toString(),
                )
            // Tarnów: 1 item (< 3 threshold, empty areas -> SocialArea.OTHER)
            val need6 =
                NeedItem(
                    text = "Brak transportu i pomocy medycznej",
                    municipality = "Tarnów",
                    areas = emptyList(),
                    matchedInnovationIds = emptyList(),
                    noGoodMatch = true,
                    createdAt = now.minus(60, ChronoUnit.DAYS).toString(),
                )

            // Previous window needs (between 6 and 12 months ago, e.g. 200-220 days ago)
            val needPrev1 =
                NeedItem(
                    text = "Poprzednie zgłoszenie samotność",
                    municipality = "Kraków",
                    areas = listOf(SocialArea.LONELINESS),
                    matchedInnovationIds = listOf(ObjectId()),
                    noGoodMatch = false,
                    createdAt = now.minus(200, ChronoUnit.DAYS).toString(),
                )
            val needPrev2 =
                NeedItem(
                    text = "Poprzednie zgłoszenie starzenie",
                    municipality = "Wieliczka",
                    areas = listOf(SocialArea.AGING),
                    matchedInnovationIds = listOf(ObjectId()),
                    noGoodMatch = false,
                    createdAt = now.minus(210, ChronoUnit.DAYS).toString(),
                )

            db.needs.insertMany(listOf(need1, need2, need3, need4, need5, need6, needPrev1, needPrev2))
            mongoClient.close()

            val response =
                client.get(AdminTrends(months = 6)) {
                    bearerAuth(adminToken)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val trends = response.body<TrendsDto>()

            // 1. By Area verification
            val loneliness = trends.byArea.find { it.area == SocialArea.LONELINESS }
            assertNotNull(loneliness)
            assertEquals(3, loneliness.count)
            assertEquals(1, loneliness.previousCount)

            val aging = trends.byArea.find { it.area == SocialArea.AGING }
            assertNotNull(aging)
            assertEquals(2, aging.count)
            assertEquals(1, aging.previousCount)

            val other = trends.byArea.find { it.area == SocialArea.OTHER }
            assertNotNull(other)
            assertEquals(1, other.count) // need6 had empty areas -> mapped to OTHER
            assertEquals(0, other.previousCount)

            // 2. By Municipality verification (privacy threshold >= 3)
            val municipalities = trends.byMunicipality.map { it.municipality }
            assertTrue("Kraków" in municipalities, "Kraków should be included since it has 3 requests")
            val krakow = trends.byMunicipality.find { it.municipality == "Kraków" }!!
            assertEquals(3, krakow.count)

            assertFalse("Wieliczka" in municipalities, "Wieliczka with 2 requests must NOT appear due to privacy")
            assertFalse("Tarnów" in municipalities, "Tarnów with 1 request must NOT appear due to privacy")

            // 3. Unmatched needs count verification
            assertEquals(3, trends.unmatchedNeeds) // need4, need5, need6

            // 4. Series verification
            assertEquals(6, trends.series.size)
            assertEquals(6, trends.series.sumOf { it.count })

            // 5. Top unmatched terms verification
            // "transport" appears in need4, need5, need6
            assertTrue("transport" in trends.topUnmatchedTerms.first().lowercase())
            // words used by fewer than 3 unmatched needs (e.g. "medyczny", in need5 only) must not show up
            assertTrue(trends.topUnmatchedTerms.none { it.lowercase().startsWith("medycz") })
            assertEquals(3, trends.privacyThreshold)
        }

    @Test
    fun trendsRejectsMonthsOutsideAllowedRange() =
        withApp { client ->
            listOf(0, -1, 61).forEach { months ->
                val response = client.get(AdminTrends(months = months)) { bearerAuth(adminToken) }
                assertEquals(HttpStatusCode.BadRequest, response.status, "months=$months")
            }
            assertEquals(HttpStatusCode.OK, client.get(AdminTrends(months = 60)) { bearerAuth(adminToken) }.status)
        }

    @Test
    fun trendsWindowStartsAtBeginningOfOldestMonthSoSeriesMatchesAreaCounts() =
        withApp { client ->
            val oldestMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(5)
            val oldestStart = oldestMonth.atDay(2).atStartOfDay(ZoneOffset.UTC).toInstant()
            val mongoClient = MongoClient.create(MongoTestEnvironment.connectionString)
            val db = mongoClient.getDatabase(databaseName)
            db.needs.insertOne(
                NeedItem(
                    text = "Zgłoszenie z początku okna",
                    areas = listOf(SocialArea.LONELINESS),
                    matchedInnovationIds = listOf(ObjectId()),
                    noGoodMatch = false,
                    createdAt = oldestStart.toString(),
                ),
            )
            mongoClient.close()

            val trends = client.get(AdminTrends(months = 6)) { bearerAuth(adminToken) }.body<TrendsDto>()

            assertEquals(1, trends.byArea.single { it.area == SocialArea.LONELINESS }.count)
            assertEquals(oldestMonth.toString(), trends.series.first().month)
            assertEquals(1, trends.series.first().count)
        }

    @Test
    fun adminCanGetDashboardSummaryCounters() =
        withApp { client ->
            val now = Instant.now()
            val mongoClient = MongoClient.create(MongoTestEnvironment.connectionString)
            val db = mongoClient.getDatabase(databaseName)

            // 1. Ideas: 2 SUBMITTED, 1 ACCEPTED
            val idea1 =
                IdeaItem(
                    authorId = "user1",
                    title = "Pomysł 1",
                    essence = "Istota 1",
                    targetGroups = listOf(TargetGroup.RESIDENTS),
                    stage = InnovationStage.IDEA,
                    status = IdeaStatus.SUBMITTED,
                    createdAt = now,
                    updatedAt = now,
                )
            val idea2 =
                IdeaItem(
                    authorId = "user2",
                    title = "Pomysł 2",
                    essence = "Istota 2",
                    targetGroups = listOf(TargetGroup.RESIDENTS),
                    stage = InnovationStage.IDEA,
                    status = IdeaStatus.SUBMITTED,
                    createdAt = now,
                    updatedAt = now,
                )
            val idea3 =
                IdeaItem(
                    authorId = "user3",
                    title = "Pomysł 3",
                    essence = "Istota 3",
                    targetGroups = listOf(TargetGroup.RESIDENTS),
                    stage = InnovationStage.IDEA,
                    status = IdeaStatus.ACCEPTED,
                    createdAt = now,
                    updatedAt = now,
                )
            db.ideas.insertMany(listOf(idea1, idea2, idea3))

            // 2. Test requests: 3 NEW, 1 ACCEPTED
            val tr1 =
                TestRequestItem(
                    innovationId = ObjectId(),
                    userId = "user1",
                    status = TestRequestStatus.NEW,
                    createdAt = now.toString(),
                    updatedAt = now.toString(),
                )
            val tr2 =
                TestRequestItem(
                    innovationId = ObjectId(),
                    userId = "user2",
                    status = TestRequestStatus.NEW,
                    createdAt = now.toString(),
                    updatedAt = now.toString(),
                )
            val tr3 =
                TestRequestItem(
                    innovationId = ObjectId(),
                    userId = "user3",
                    status = TestRequestStatus.NEW,
                    createdAt = now.toString(),
                    updatedAt = now.toString(),
                )
            val tr4 =
                TestRequestItem(
                    innovationId = ObjectId(),
                    userId = "user4",
                    status = TestRequestStatus.ACCEPTED,
                    createdAt = now.toString(),
                    updatedAt = now.toString(),
                )
            db.testRequests.insertMany(listOf(tr1, tr2, tr3, tr4))

            // 3. Needs:
            // 2 unmatched created within last 7 days (e.g. 2 and 3 days ago)
            // 1 unmatched created 10 days ago (older than a week)
            // 1 matched created 1 day ago
            val n1 =
                NeedItem(
                    text = "Brak pomocy 1",
                    noGoodMatch = true,
                    matchedInnovationIds = emptyList(),
                    createdAt = now.minus(2, ChronoUnit.DAYS).toString(),
                )
            val n2 =
                NeedItem(
                    text = "Brak pomocy 2",
                    noGoodMatch = true,
                    matchedInnovationIds = emptyList(),
                    createdAt = now.minus(3, ChronoUnit.DAYS).toString(),
                )
            val n3 =
                NeedItem(
                    text = "Brak pomocy stara",
                    noGoodMatch = true,
                    matchedInnovationIds = emptyList(),
                    createdAt = now.minus(10, ChronoUnit.DAYS).toString(),
                )
            val n4 =
                NeedItem(
                    text = "Dopasowana potrzeba",
                    noGoodMatch = false,
                    matchedInnovationIds = listOf(ObjectId()),
                    createdAt = now.minus(1, ChronoUnit.DAYS).toString(),
                )
            db.needs.insertMany(listOf(n1, n2, n3, n4))

            // 4. Threads:
            // Thread 1: user created, last message by user (AUTHOR) -> pending
            val thread1 =
                ThreadItem(
                    ownerId = "user1",
                    ownerName = "User 1",
                    subject = "Pytanie 1",
                    lastMessageAt = now,
                    lastMessageBy = "user1",
                    lastMessageRole = ParticipantRole.AUTHOR,
                    createdAt = now,
                    updatedAt = now,
                )
            // Thread 2: admin replied -> not pending (an official answered)
            val thread2 =
                ThreadItem(
                    ownerId = "user2",
                    ownerName = "User 2",
                    subject = "Pytanie 2",
                    lastMessageAt = now,
                    lastMessageBy = "admin",
                    lastMessageRole = ParticipantRole.ADMIN,
                    createdAt = now,
                    updatedAt = now,
                )
            // Thread 3: expert replied -> not pending, experts answer in the name of ROPS
            val thread3 =
                ThreadItem(
                    ownerId = "user3",
                    ownerName = "User 3",
                    subject = "Pytanie 3",
                    lastMessageAt = now,
                    lastMessageBy = "expert1",
                    lastMessageRole = ParticipantRole.EXPERT,
                    createdAt = now,
                    updatedAt = now,
                )
            db.threads.insertMany(listOf(thread1, thread2, thread3))
            mongoClient.close()

            val response =
                client.get(AdminSummary()) {
                    bearerAuth(adminToken)
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val summary = response.body<AdminSummaryDto>()

            assertEquals(2, summary.submittedIdeas)
            assertEquals(3, summary.pendingTestRequests)
            assertEquals(2, summary.unmatchedNeedsThisWeek)
            assertEquals(1, summary.pendingThreads)
        }
}
