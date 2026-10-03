package io.github.mfabisiak.hubmi.tester

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.AdminFeedback
import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.AdminTestRequests
import io.github.mfabisiak.hubmi.api.CreateFeedbackRequest
import io.github.mfabisiak.hubmi.api.CreateTestRequest
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.FeedbackDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestDto
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.UpdateTestRequestStatusRequest
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.koin.dsl.module
import kotlin.test.*

class TesterRouteTest {
    private val databaseName = "test-hubmi-tester"
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

    private suspend fun HttpClient.createInnovation(): InnovationDto =
        post(Innovations()) {
            contentType(ContentType.Application.Json)
            bearerAuth(adminToken)
            setBody(
                UpsertInnovationRequest(
                    title = "Klub sąsiedzki ${System.nanoTime()}",
                    summary = "Spotkania sąsiadów.",
                    description = "Cotygodniowe spotkania sąsiadów w świetlicy osiedlowej.",
                    areas = listOf(SocialArea.LONELINESS),
                    targetGroups = listOf(TargetGroup.RESIDENTS),
                    stage = InnovationStage.PILOT,
                ),
            )
        }.body()

    private suspend fun HttpClient.rate(
        innovationId: String,
        token: String?,
        rating: Int,
        comment: String? = null,
        suggestion: String? = null,
    ) = put(Innovations.ById.Feedback(Innovations.ById(id = innovationId))) {
        contentType(ContentType.Application.Json)
        token?.let { bearerAuth(it) }
        setBody(CreateFeedbackRequest(rating, comment, suggestion))
    }

    private suspend fun HttpClient.requestTest(
        innovationId: String,
        token: String?,
        note: String? = null,
    ) = put(Innovations.ById.TestRequest(Innovations.ById(id = innovationId))) {
        contentType(ContentType.Application.Json)
        token?.let { bearerAuth(it) }
        setBody(CreateTestRequest(note))
    }

    private suspend fun HttpClient.fetch(innovationId: String): InnovationDto =
        get(Innovations.ById(id = innovationId)).body()

    @Test
    fun bothEndpointsRequireAToken() =
        withApp { client ->
            val innovation = client.createInnovation()

            assertEquals(HttpStatusCode.Unauthorized, client.rate(innovation.id, token = null, rating = 5).status)
            assertEquals(HttpStatusCode.Unauthorized, client.requestTest(innovation.id, token = null).status)
        }

    @Test
    fun ratingIsStoredAndShownInTheInnovation() =
        withApp { client ->
            val innovation = client.createInnovation()
            assertNull(client.fetch(innovation.id).averageRating)

            val response =
                client.rate(
                    innovation.id,
                    alice,
                    rating = 4,
                    comment = "  Świetny pomysł ",
                    suggestion = "Więcej miejsc",
                )

            assertEquals(HttpStatusCode.OK, response.status)
            val feedback = response.body<FeedbackDto>()
            assertEquals(innovation.id, feedback.innovationId)
            assertEquals(4, feedback.rating)
            assertEquals("Świetny pomysł", feedback.comment)
            assertEquals("Więcej miejsc", feedback.suggestion)
            val fetched = client.fetch(innovation.id)
            assertEquals(4.0, fetched.averageRating)
            assertEquals(1, fetched.ratingsCount)
        }

    @Test
    fun ratingAgainOverwritesTheUsersRatingInsteadOfAddingASecondOne() =
        withApp { client ->
            val innovation = client.createInnovation()
            client.rate(innovation.id, alice, rating = 5)
            client.rate(innovation.id, bob, rating = 3)
            assertEquals(4.0, client.fetch(innovation.id).averageRating)
            assertEquals(2, client.fetch(innovation.id).ratingsCount)

            val again = client.rate(innovation.id, alice, rating = 1, comment = "Zmieniłam zdanie")

            assertEquals(HttpStatusCode.OK, again.status)
            val fetched = client.fetch(innovation.id)
            assertEquals(2.0, fetched.averageRating, "average moves by the delta of Alice's change, not by a new vote")
            assertEquals(2, fetched.ratingsCount)
            assertEquals("Zmieniłam zdanie", again.body<FeedbackDto>().comment)
        }

    @Test
    fun feedbackKeepsItsCreationTimeAndIdWhenOverwritten() =
        withApp { client ->
            val innovation = client.createInnovation()
            val first = client.rate(innovation.id, alice, rating = 2).body<FeedbackDto>()

            val second = client.rate(innovation.id, alice, rating = 5).body<FeedbackDto>()

            assertEquals(first.id, second.id)
            assertEquals(first.createdAt, second.createdAt)
        }

    @Test
    fun invalidFeedbackIsRejectedWithFieldDetails() =
        withApp { client ->
            val innovation = client.createInnovation()

            listOf(0, 6, -1).forEach { rating ->
                val response = client.rate(innovation.id, alice, rating)
                assertEquals(HttpStatusCode.BadRequest, response.status, "rating $rating")
                assertTrue(response.body<ErrorResponse>().details.any { it.field == "rating" })
            }
            val tooLong =
                client.rate(
                    innovation.id,
                    alice,
                    rating = 3,
                    comment = "x".repeat(1001),
                    suggestion = "y".repeat(1001),
                )
            assertEquals(HttpStatusCode.BadRequest, tooLong.status)
            assertEquals(
                setOf("comment", "suggestion"),
                tooLong
                    .body<ErrorResponse>()
                    .details
                    .map { it.field }
                    .toSet(),
            )
            assertNull(client.fetch(innovation.id).averageRating, "rejected feedback must not change the aggregate")
        }

    @Test
    fun blankCommentIsTreatedAsNoComment() =
        withApp { client ->
            val innovation = client.createInnovation()

            val feedback =
                client
                    .rate(
                        innovation.id,
                        alice,
                        rating = 3,
                        comment = "   ",
                        suggestion = "",
                    ).body<FeedbackDto>()

            assertNull(feedback.comment)
            assertNull(feedback.suggestion)
        }

    @Test
    fun unknownOrDeletedInnovationIsNotFoundAndMalformedIdIsBadRequest() =
        withApp { client ->
            val deleted = client.createInnovation()
            client.delete(Innovations.ById(id = deleted.id)) { bearerAuth(adminToken) }

            listOf(ObjectId().toHexString(), deleted.id).forEach { id ->
                assertEquals(HttpStatusCode.NotFound, client.rate(id, alice, rating = 5).status)
                assertEquals(HttpStatusCode.NotFound, client.requestTest(id, alice).status)
            }
            assertEquals(HttpStatusCode.BadRequest, client.rate("not-an-id", alice, rating = 5).status)
            assertEquals(HttpStatusCode.BadRequest, client.requestTest("not-an-id", alice).status)
            val error = client.rate(ObjectId().toHexString(), alice, rating = 5).body<ErrorResponse>()
            assertEquals(ErrorCode.NOT_FOUND, error.code)
        }

    @Test
    fun ratingsOfDifferentInnovationsAreIndependent() =
        withApp { client ->
            val first = client.createInnovation()
            val second = client.createInnovation()

            client.rate(first.id, alice, rating = 5)
            client.rate(second.id, alice, rating = 2)

            assertEquals(5.0, client.fetch(first.id).averageRating)
            assertEquals(2.0, client.fetch(second.id).averageRating)
        }

    @Test
    fun testRequestIsIdempotentAndReplacesTheNoteWhileNew() =
        withApp { client ->
            val innovation = client.createInnovation()

            val first = client.requestTest(innovation.id, alice, note = "  Mogę testować w weekendy ")
            assertEquals(HttpStatusCode.OK, first.status)
            val created = first.body<TestRequestDto>()
            assertEquals(innovation.id, created.innovationId)
            assertEquals("Mogę testować w weekendy", created.note)
            assertEquals(TestRequestStatus.NEW, created.status)

            val same =
                client
                    .requestTest(
                        innovation.id,
                        alice,
                        note = "  Mogę testować w weekendy ",
                    ).body<TestRequestDto>()
            assertEquals(created, same, "repeating an identical PUT changes nothing")

            val replaced = client.requestTest(innovation.id, alice, note = "Wolę wieczory").body<TestRequestDto>()
            assertEquals(created.id, replaced.id)
            assertEquals("Wolę wieczory", replaced.note)
            assertEquals(created.createdAt, replaced.createdAt)

            val other = client.requestTest(innovation.id, bob).body<TestRequestDto>()
            assertNotEquals(created.id, other.id)
            assertNull(other.note)
        }

    @Test
    fun overlongTestRequestNoteIsRejected() =
        withApp { client ->
            val innovation = client.createInnovation()

            val response = client.requestTest(innovation.id, alice, note = "x".repeat(1001))

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.body<ErrorResponse>().details.any { it.field == "note" })
        }

    private suspend fun HttpClient.adminTestRequests(
        innovationId: String? = null,
        status: TestRequestStatus? = null,
        token: String? = adminToken,
    ) = get(AdminTestRequests(innovationId = innovationId, status = status)) { token?.let { bearerAuth(it) } }

    private suspend fun HttpClient.decide(
        id: String,
        status: TestRequestStatus,
        token: String? = adminToken,
    ) = patch(AdminTestRequests.Status(id = id)) {
        contentType(ContentType.Application.Json)
        token?.let { bearerAuth(it) }
        setBody(UpdateTestRequestStatusRequest(status))
    }

    @Test
    fun adminSeesFeedbackWithInnovationTitlesAndCanFilterIt() =
        withApp { client ->
            val first = client.createInnovation()
            val second = client.createInnovation()
            client.rate(first.id, alice, rating = 5, comment = "Super")
            client.rate(first.id, bob, rating = 3)
            client.rate(second.id, alice, rating = 2, suggestion = "Więcej miejsc")

            val all = client.get(AdminFeedback()) { bearerAuth(adminToken) }.body<Page<AdminFeedbackDto>>()
            assertEquals(3, all.total)
            assertEquals(listOf(second.id, first.id, first.id), all.items.map { it.innovationId }, "newest first")
            assertEquals(second.title, all.items.first().innovationTitle)
            assertEquals("alice", all.items.first().userId)
            assertEquals("Więcej miejsc", all.items.first().suggestion)

            val filtered =
                client
                    .get(
                        AdminFeedback(innovationId = first.id),
                    ) { bearerAuth(adminToken) }
                    .body<Page<AdminFeedbackDto>>()
            assertEquals(setOf("alice", "bob"), filtered.items.map { it.userId }.toSet())
            assertTrue(filtered.items.all { it.innovationTitle == first.title })

            val paged =
                client
                    .get(
                        AdminFeedback(page = 1, size = 2),
                    ) { bearerAuth(adminToken) }
                    .body<Page<AdminFeedbackDto>>()
            assertEquals(1, paged.items.size)
            assertEquals(3, paged.total)
        }

    @Test
    fun feedbackOfARemovedInnovationIsStillLabelledForTheAdmin() =
        withApp { client ->
            val innovation = client.createInnovation()
            client.rate(innovation.id, alice, rating = 4)
            client.delete(Innovations.ById(id = innovation.id)) { bearerAuth(adminToken) }

            val page = client.get(AdminFeedback()) { bearerAuth(adminToken) }.body<Page<AdminFeedbackDto>>()

            assertEquals(innovation.title, page.items.single().innovationTitle)
        }

    @Test
    fun adminEndpointsRejectUsersAndMalformedInput() =
        withApp { client ->
            assertEquals(HttpStatusCode.Unauthorized, client.get(AdminFeedback()).status)
            assertEquals(HttpStatusCode.Forbidden, client.get(AdminFeedback()) { bearerAuth(alice) }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.adminTestRequests(token = null).status)
            assertEquals(HttpStatusCode.Forbidden, client.adminTestRequests(token = alice).status)
            assertEquals(
                HttpStatusCode.BadRequest,
                client
                    .get(AdminFeedback(innovationId = "nope")) {
                        bearerAuth(adminToken)
                    }.status,
            )
            assertEquals(HttpStatusCode.BadRequest, client.adminTestRequests(innovationId = "nope").status)
            assertEquals(
                HttpStatusCode.BadRequest,
                client.get(AdminFeedback(size = 0)) { bearerAuth(adminToken) }.status,
            )
            assertEquals(
                HttpStatusCode.BadRequest,
                client
                    .get("/api/admin/test-requests?status=NIE_ISTNIEJE") {
                        bearerAuth(adminToken)
                    }.status,
            )
        }

    @Test
    fun adminListsAndFiltersTestRequests() =
        withApp { client ->
            val innovation = client.createInnovation()
            val other = client.createInnovation()
            val aliceRequest = client.requestTest(innovation.id, alice, note = "Chętnie").body<TestRequestDto>()
            client.requestTest(innovation.id, bob)
            client.requestTest(other.id, alice)
            client.decide(aliceRequest.id, TestRequestStatus.ACCEPTED)

            val all = client.adminTestRequests().body<Page<AdminTestRequestDto>>()
            assertEquals(3, all.total)
            val forInnovation = client.adminTestRequests(innovationId = innovation.id).body<Page<AdminTestRequestDto>>()
            assertEquals(setOf("alice", "bob"), forInnovation.items.map { it.userId }.toSet())
            assertTrue(forInnovation.items.all { it.innovationTitle == innovation.title })
            val accepted =
                client
                    .adminTestRequests(
                        status = TestRequestStatus.ACCEPTED,
                    ).body<Page<AdminTestRequestDto>>()
            assertEquals(listOf(aliceRequest.id), accepted.items.map { it.id })
            assertEquals("Chętnie", accepted.items.single().note)
            assertEquals(
                2,
                client.adminTestRequests(status = TestRequestStatus.NEW).body<Page<AdminTestRequestDto>>().total,
            )
        }

    @Test
    fun aTestRequestIsDecidedOnceAndThenFrozen() =
        withApp { client ->
            val innovation = client.createInnovation()
            val request = client.requestTest(innovation.id, alice, note = "Chętnie").body<TestRequestDto>()

            val accepted = client.decide(request.id, TestRequestStatus.ACCEPTED)
            assertEquals(HttpStatusCode.OK, accepted.status)
            val decided = accepted.body<AdminTestRequestDto>()
            assertEquals(TestRequestStatus.ACCEPTED, decided.status)
            assertEquals(innovation.title, decided.innovationTitle)
            assertEquals("alice", decided.userId)

            assertEquals(HttpStatusCode.Conflict, client.decide(request.id, TestRequestStatus.DECLINED).status)
            assertEquals(HttpStatusCode.Conflict, client.decide(request.id, TestRequestStatus.ACCEPTED).status)
            assertEquals(HttpStatusCode.Conflict, client.decide(request.id, TestRequestStatus.NEW).status)
            val frozen = client.requestTest(innovation.id, alice, note = "Zmieniam zdanie")
            assertEquals(HttpStatusCode.Conflict, frozen.status)
            assertEquals(ErrorCode.CONFLICT, frozen.body<ErrorResponse>().code)
            val stored =
                client
                    .adminTestRequests()
                    .body<Page<AdminTestRequestDto>>()
                    .items
                    .single()
            assertEquals("Chętnie", stored.note, "a rejected change must not be stored")
            assertEquals(TestRequestStatus.ACCEPTED, stored.status)
        }

    @Test
    fun decidingValidatesAccessAndTheTarget() =
        withApp { client ->
            val innovation = client.createInnovation()
            val request = client.requestTest(innovation.id, alice).body<TestRequestDto>()

            assertEquals(
                HttpStatusCode.Unauthorized,
                client.decide(request.id, TestRequestStatus.ACCEPTED, token = null).status,
            )
            assertEquals(
                HttpStatusCode.Forbidden,
                client.decide(request.id, TestRequestStatus.ACCEPTED, token = alice).status,
            )
            assertEquals(
                HttpStatusCode.NotFound,
                client.decide(ObjectId().toHexString(), TestRequestStatus.ACCEPTED).status,
            )
            assertEquals(HttpStatusCode.BadRequest, client.decide("nope", TestRequestStatus.ACCEPTED).status)
            assertEquals(
                HttpStatusCode.Conflict,
                client.decide(request.id, TestRequestStatus.NEW).status,
                "NEW is not a decision",
            )
            assertEquals(
                TestRequestStatus.NEW,
                client
                    .adminTestRequests()
                    .body<Page<AdminTestRequestDto>>()
                    .items
                    .single()
                    .status,
            )

            val declined = client.decide(request.id, TestRequestStatus.DECLINED)
            assertEquals(TestRequestStatus.DECLINED, declined.body<AdminTestRequestDto>().status)
        }

    @Test
    fun parallelRatingsLeaveTheAggregateConsistent() =
        withApp { client ->
            val innovation = client.createInnovation()
            val users = (1..12).map { TestSecurityHelper.generateToken(userId = "user-$it", username = "user-$it") }

            kotlinx.coroutines.coroutineScope {
                users
                    .mapIndexed {
                        index,
                        token,
                        ->
                        async { client.rate(innovation.id, token, rating = index % 5 + 1) }
                    }.awaitAll()
            }
            // one more call refreshes the aggregate from the stored ratings, whatever the interleaving was
            client.rate(innovation.id, alice, rating = 3)

            val fetched = client.fetch(innovation.id)
            assertEquals(13, fetched.ratingsCount)
            val expected = users.indices.sumOf { it % 5 + 1 } + 3
            assertEquals(expected.toDouble() / 13, fetched.averageRating)
        }

    @Test
    fun usersReadBackTheirOwnRatingAndTestRequestWithItsStatus() =
        withApp { client ->
            val innovation = client.createInnovation()
            val feedbackUrl = Innovations.ById.Feedback(Innovations.ById(id = innovation.id))
            val requestUrl = Innovations.ById.TestRequest(Innovations.ById(id = innovation.id))

            assertEquals(HttpStatusCode.NotFound, client.get(feedbackUrl) { bearerAuth(alice) }.status)
            assertEquals(HttpStatusCode.NotFound, client.get(requestUrl) { bearerAuth(alice) }.status)

            val rated = client.rate(innovation.id, alice, rating = 4, comment = "Dobre").body<FeedbackDto>()
            val requested = client.requestTest(innovation.id, alice, note = "Chętnie").body<TestRequestDto>()
            assertEquals(rated, client.get(feedbackUrl) { bearerAuth(alice) }.body<FeedbackDto>())
            assertEquals(requested, client.get(requestUrl) { bearerAuth(alice) }.body<TestRequestDto>())

            client.decide(requested.id, TestRequestStatus.ACCEPTED)
            assertEquals(
                TestRequestStatus.ACCEPTED,
                client.get(requestUrl) { bearerAuth(alice) }.body<TestRequestDto>().status,
            )

            assertEquals(
                HttpStatusCode.NotFound,
                client.get(feedbackUrl) { bearerAuth(bob) }.status,
                "never someone else's",
            )
            assertEquals(HttpStatusCode.NotFound, client.get(requestUrl) { bearerAuth(bob) }.status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(feedbackUrl).status)
            assertEquals(HttpStatusCode.Unauthorized, client.get(requestUrl).status)
            assertEquals(
                HttpStatusCode.BadRequest,
                client.get("/api/innovations/nope/feedback") { bearerAuth(alice) }.status,
            )
        }
}
