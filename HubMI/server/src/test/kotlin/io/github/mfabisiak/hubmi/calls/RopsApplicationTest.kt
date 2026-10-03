package io.github.mfabisiak.hubmi.calls

import io.github.mfabisiak.hubmi.MongoTestEnvironment
import io.github.mfabisiak.hubmi.TestSecurityHelper
import io.github.mfabisiak.hubmi.api.ActionPlanDto
import io.github.mfabisiak.hubmi.api.AddressDto
import io.github.mfabisiak.hubmi.api.ApplicantDto
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.Applications
import io.github.mfabisiak.hubmi.api.Calls
import io.github.mfabisiak.hubmi.api.ContactPersonDto
import io.github.mfabisiak.hubmi.api.CreateApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.EntityApplicantDto
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IndividualApplicantDto
import io.github.mfabisiak.hubmi.api.PlanItemDto
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.module
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.koin.dsl.module
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.*

class RopsApplicationTest {
    private val fixedNow = Instant.parse("2026-06-01T12:00:00Z")
    private val testClock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    private val testModule =
        module {
            single {
                AppConfig(
                    port = 8080,
                    keycloakIssuer = TestSecurityHelper.ISSUER,
                    keycloakJwksUrl = "http://localhost:8081/realms/hubmi/protocol/openid-connect/certs",
                    mongoUri = MongoTestEnvironment.connectionString,
                    mongoDatabase = "test-hubmi-rops-${System.nanoTime()}",
                    seed = false,
                )
            }
            single { TestSecurityHelper.testJwkProvider }
            single<Clock> { testClock }
        }

    private fun ApplicationTestBuilder.createJsonClient() =
        createClient {
            install(Resources)
            install(ContentNegotiation) { json() }
        }

    private val validIndividual =
        IndividualApplicantDto(
            firstName = "Jan",
            lastName = "Kowalski",
            address =
                AddressDto(
                    street = "Floriańska",
                    buildingNumber = "10",
                    apartmentNumber = "2",
                    postalCode = "31-019",
                    city = "Kraków",
                ),
            phone = "123456789",
            email = "jan.kowalski@example.com",
        )

    private val validEntity =
        EntityApplicantDto(
            name = "Fundacja Innowacji Społecznych",
            krs = "0000123456",
            regon = "123456785",
            nip = "1234563218",
            address =
                AddressDto(
                    street = "Grodzka",
                    buildingNumber = "5",
                    postalCode = "31-006",
                    city = "Kraków",
                ),
            phone = "124210000",
            email = "kontakt@fundacja.pl",
            representative =
                ContactPersonDto(
                    function = "Prezes Zarządu",
                    fullName = "Anna Nowak",
                    phone = "987654321",
                    email = "anna.nowak@fundacja.pl",
                ),
            contactPerson =
                ContactPersonDto(
                    function = "Koordynator Projektu",
                    fullName = "Piotr Wiśniewski",
                    phone = "555666777",
                    email = "piotr.w@fundacja.pl",
                ),
        )

    private val validPlan =
        ActionPlanDto(
            preparation =
                listOf(
                    PlanItemDto("Diagnoza i rekrutacja", "2026-06", 500_000), // 5 000,00 zł
                    PlanItemDto("Opracowanie materiałów", "2026-07", 500_000),
                ),
            testingPhase1 =
                listOf(
                    PlanItemDto("Faza I: pilotaż warsztatów", "2026-08", 1_000_000),
                    PlanItemDto("Faza I: ewaluacja wstępna", "2026-10", 500_000),
                ),
            testingPhase2 =
                listOf(
                    PlanItemDto("Faza II: wdrożenie rozszerzone", "2026-11", 1_500_000),
                    PlanItemDto("Faza II: raport końcowy", "2027-04", 1_000_000),
                ),
        )

    // Total cost = 500_000 + 500_000 + 1_000_000 + 500_000 + 1_500_000 + 1_000_000 = 5_000_000 gr (50 000 PLN)
    private val validTotalCostGrosze = 5_000_000

    private fun createValidDraftRequest(applicant: ApplicantDto) =
        SaveApplicationDraftRequest(
            title = "Innowacyjna Platforma Włączenia Seniorów",
            applicant = applicant,
            description = "Aplikacja łącząca seniorów z lokalnymi wolontariuszami i asystentami.",
            innovativeness = "Zastosowanie mikromatching'u sąsiedzkiego i prostego interfejsu głosowego.",
            problemDiagnosis = "Izolacja społeczna seniorów w gminach wiejskich Małopolski.",
            socialArea = SocialArea.AGING,
            audienceDescription = "Seniorzy 65+ mieszkający samotnie oraz lokalni wolontariusze.",
            expectedChange = "Zwiększenie aktywności i poczucia bezpieczeństwa 200 seniorów.",
            futureVision = "Skalowanie rozwiązania na sąsiednie powiaty po zakończeniu testu.",
            plan = validPlan,
            requestedGrantAmountGrosze = validTotalCostGrosze,
            projectTeam = "Koordynator z 10-letnim doświadczeniem w projektach społecznych, psycholog, programista.",
            declarations = RopsDeclarations.getRequiredIds(applicant.type).toList(),
        )

    @Test
    fun checksumValidationUnitTests() {
        // NIP
        assertTrue(RopsApplicationValidator.isValidNip("1234563218"))
        assertFalse(RopsApplicationValidator.isValidNip("1234563219"))
        assertFalse(RopsApplicationValidator.isValidNip("123"))

        // REGON 9
        assertTrue(RopsApplicationValidator.isValidRegon("123456785"))
        assertFalse(RopsApplicationValidator.isValidRegon("123456789"))

        // KRS
        assertTrue(RopsApplicationValidator.isValidKrs("0000123456"))
        assertFalse(RopsApplicationValidator.isValidKrs("123456"))
        assertFalse(RopsApplicationValidator.isValidKrs("00001234567"))
    }

    @Test
    fun fullWorkflowDraftEditAndSubmitAllThreeApplicantVariants() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val userToken = TestSecurityHelper.generateToken(userId = "applicant-rops-1", roles = setOf(Role.USER))

            // 1. Admin creates an open call
            val callResponse =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertCallRequest(
                            title = "Inkubator Włączenia Społecznego 2.0",
                            description = "Nabór na mikroinnowacje ROPS",
                            opensAt = fixedNow.minus(1, ChronoUnit.DAYS).toString(),
                            closesAt = fixedNow.plus(30, ChronoUnit.DAYS).toString(),
                            fields = emptyList(),
                        ),
                    )
                }
            val call = callResponse.body<GrantCallDto>()

            // 2. Individual applicant workflow
            val indivDraftResponse =
                client.post(Calls.ById.Applications(Calls.ById(id = call.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            assertEquals(HttpStatusCode.Created, indivDraftResponse.status)
            val indivDraft = indivDraftResponse.body<ApplicationDto>()
            assertEquals(ApplicationStatus.DRAFT, indivDraft.status)

            // Save draft
            val putResponse =
                client.put(Applications.ById(id = indivDraft.id)) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(createValidDraftRequest(validIndividual))
                }
            assertEquals(HttpStatusCode.OK, putResponse.status)

            // Submit application
            val submitResponse =
                client.post(Applications.ById.Submit(Applications.ById(id = indivDraft.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            assertEquals(HttpStatusCode.OK, submitResponse.status)
            val submittedApp = submitResponse.body<ApplicationDto>()
            assertEquals(ApplicationStatus.SUBMITTED, submittedApp.status)
            assertEquals("Jan", (submittedApp.applicant as? IndividualApplicantDto)?.firstName)

            // 3. Immutability check: cannot edit submitted application (409 Conflict)
            val editSubmittedResponse =
                client.put(Applications.ById(id = indivDraft.id)) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(createValidDraftRequest(validIndividual).copy(title = "Próba zmiany po złożeniu"))
                }
            assertEquals(HttpStatusCode.Conflict, editSubmittedResponse.status)

            // 4. Entity applicant workflow (second allowed submission for this user)
            val entityDraftResponse =
                client.post(Calls.ById.Applications(Calls.ById(id = call.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            val entityDraft = entityDraftResponse.body<ApplicationDto>()
            client.put(Applications.ById(id = entityDraft.id)) {
                header(HttpHeaders.Authorization, "Bearer $userToken")
                contentType(ContentType.Application.Json)
                setBody(createValidDraftRequest(validEntity))
            }
            val submitEntityResponse =
                client.post(Applications.ById.Submit(Applications.ById(id = entityDraft.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            assertEquals(HttpStatusCode.OK, submitEntityResponse.status)

            // 5. Limit check: 3rd application in same call -> 409 Conflict
            val thirdDraftResponse =
                client.post(Calls.ById.Applications(Calls.ById(id = call.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            val thirdDraft = thirdDraftResponse.body<ApplicationDto>()
            client.put(Applications.ById(id = thirdDraft.id)) {
                header(HttpHeaders.Authorization, "Bearer $userToken")
                contentType(ContentType.Application.Json)
                setBody(createValidDraftRequest(validIndividual))
            }
            val submitThirdResponse =
                client.post(Applications.ById.Submit(Applications.ById(id = thirdDraft.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            assertEquals(HttpStatusCode.Conflict, submitThirdResponse.status)
        }

    @Test
    fun budgetAndDurationValidation() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val userToken = TestSecurityHelper.generateToken(userId = "applicant-rops-val", roles = setOf(Role.USER))

            val callResponse =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertCallRequest(
                            title = "Nabór Walidacja",
                            description = "Opis",
                            opensAt = fixedNow.minus(1, ChronoUnit.DAYS).toString(),
                            closesAt = fixedNow.plus(30, ChronoUnit.DAYS).toString(),
                            fields = emptyList(),
                        ),
                    )
                }
            val call = callResponse.body<GrantCallDto>()

            val draftResponse =
                client.post(Calls.ById.Applications(Calls.ById(id = call.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            val draft = draftResponse.body<ApplicationDto>()

            // 1. Budget sum mismatch -> 400 Bad Request
            val mismatchedBudget = createValidDraftRequest(validIndividual).copy(requestedGrantAmountGrosze = 999_999)
            client.put(Applications.ById(id = draft.id)) {
                header(HttpHeaders.Authorization, "Bearer $userToken")
                contentType(ContentType.Application.Json)
                setBody(mismatchedBudget)
            }
            val submitBudgetMismatch =
                client.post(Applications.ById.Submit(Applications.ById(id = draft.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            assertEquals(HttpStatusCode.BadRequest, submitBudgetMismatch.status)

            // 2. Preparation duration > 3 months -> 400 Bad Request
            val excessivePrepPlan =
                validPlan.copy(
                    preparation =
                        listOf(
                            PlanItemDto("D1", "2026-06", 2_500_000),
                            PlanItemDto("D2", "2026-10", 2_500_000), // 5 months!
                        ),
                    testingPhase1 = listOf(PlanItemDto("T1", "2026-11", 0)),
                )
            client.put(Applications.ById(id = draft.id)) {
                header(HttpHeaders.Authorization, "Bearer $userToken")
                contentType(ContentType.Application.Json)
                setBody(
                    createValidDraftRequest(validIndividual).copy(
                        plan = excessivePrepPlan,
                        requestedGrantAmountGrosze = 5_000_000,
                    ),
                )
            }
            val submitExcessivePrep =
                client.post(Applications.ById.Submit(Applications.ById(id = draft.id))) {
                    header(HttpHeaders.Authorization, "Bearer $userToken")
                }
            assertEquals(HttpStatusCode.BadRequest, submitExcessivePrep.status)
        }

    @Test
    fun accessControlForeignApplicationCannotBeAccessed() =
        testApplication {
            application { module(testModule) }
            val client = createJsonClient()
            val adminToken = TestSecurityHelper.generateToken(roles = setOf(Role.ADMIN))
            val user1Token = TestSecurityHelper.generateToken(userId = "user-1", roles = setOf(Role.USER))
            val user2Token = TestSecurityHelper.generateToken(userId = "user-2", roles = setOf(Role.USER))

            val callResponse =
                client.post(Calls()) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        UpsertCallRequest(
                            title = "Nabór Dostęp",
                            description = "Opis",
                            opensAt = fixedNow.minus(1, ChronoUnit.DAYS).toString(),
                            closesAt = fixedNow.plus(30, ChronoUnit.DAYS).toString(),
                            fields = emptyList(),
                        ),
                    )
                }
            val call = callResponse.body<GrantCallDto>()

            val draftResponse =
                client.post(Calls.ById.Applications(Calls.ById(id = call.id))) {
                    header(HttpHeaders.Authorization, "Bearer $user1Token")
                    contentType(ContentType.Application.Json)
                    setBody(CreateApplicationDraftRequest())
                }
            val draft = draftResponse.body<ApplicationDto>()

            // User 2 cannot read user 1's application -> 403 Forbidden
            val foreignGet =
                client.get(Applications.ById(id = draft.id)) {
                    header(HttpHeaders.Authorization, "Bearer $user2Token")
                }
            assertEquals(HttpStatusCode.Forbidden, foreignGet.status)

            // User 2 cannot edit user 1's application -> 403 Forbidden
            val foreignPut =
                client.put(Applications.ById(id = draft.id)) {
                    header(HttpHeaders.Authorization, "Bearer $user2Token")
                    contentType(ContentType.Application.Json)
                    setBody(createValidDraftRequest(validIndividual))
                }
            assertEquals(HttpStatusCode.Forbidden, foreignPut.status)

            // Admin CAN read user 1's application -> 200 OK
            val adminGet =
                client.get(Applications.ById(id = draft.id)) {
                    header(HttpHeaders.Authorization, "Bearer $adminToken")
                }
            assertEquals(HttpStatusCode.OK, adminGet.status)
        }
}
