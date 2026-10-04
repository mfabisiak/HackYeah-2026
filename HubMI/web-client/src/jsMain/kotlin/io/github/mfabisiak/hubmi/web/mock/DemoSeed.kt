package io.github.mfabisiak.hubmi.web.mock

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.ActionPlanDto
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationPlanDto
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.AdaptationStepDto
import io.github.mfabisiak.hubmi.api.AddressDto
import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.CallStatus
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.IdeaStatus
import io.github.mfabisiak.hubmi.api.IndividualApplicantDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.InstitutionProfile
import io.github.mfabisiak.hubmi.api.InstitutionType
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.NotificationType
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.PlanItemDto
import io.github.mfabisiak.hubmi.api.RopsDeclarations
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.TestRequestStatus
import io.github.mfabisiak.hubmi.api.ThreadDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private class SeedInnovation(
    val slug: String,
    val title: String,
    val summary: String,
    val description: String,
    val areas: List<SocialArea>,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
    val region: String? = null,
    val keywords: List<String> = emptyList(),
    val mediaUrls: List<String> = emptyList(),
    val innovativeness: String? = null,
    val problemDiagnosis: String? = null,
    val audienceDescription: String? = null,
    val expectedChange: String? = null,
    val futureVision: String? = null,
)

@Serializable
private class SeedChallenge(
    val slug: String,
    val title: String,
    val description: String,
    val area: SocialArea,
    val municipalities: List<String> = emptyList(),
)

@Serializable
private class SeedMaterial(
    val slug: String,
    val title: String,
    val description: String,
    val type: MaterialType,
    val url: String,
    val areas: List<SocialArea> = emptyList(),
)

private val json = Json { ignoreUnknownKeys = true }

private inline fun <reified T> readSeed(
    text: String,
    serializer: kotlinx.serialization.KSerializer<List<T>>,
): List<T> = Either.catch { json.decodeFromString(serializer, text) }.getOrNull().orEmpty()

private val seedInnovations: List<SeedInnovation> =
    readSeed(INNOVATIONS_JSON, ListSerializer(SeedInnovation.serializer()))

/** The innovations, challenges and materials are the very ones the server seeds its database with. */
internal object DemoSeed {
    /** Search keywords of the seeded innovations, by id. */
    val keywords: Map<String, List<String>> = seedInnovations.associate { it.slug to it.keywords }

    private const val INBOX_CALL = "nabor-inkubator-2026"
    private const val SENIORS_CALL = "nabor-seniorzy-2027"
    private const val CLOSED_CALL = "nabor-wlaczajace-2025"

    fun initial(): MockDb {
        val innovations = seedInnovations.map { it.toDto() }
        return MockDb(
            innovations = innovations,
            challenges = readSeed(CHALLENGES_JSON, ListSerializer(SeedChallenge.serializer())).map { it.toDto() },
            materials = readSeed(MATERIALS_JSON, ListSerializer(SeedMaterial.serializer())).map { it.toDto() },
            feedback = feedback(innovations),
            testRequests = testRequests(innovations),
            ideas = ideas(),
            calls = calls(),
            applications = applications(),
            adaptations = adaptations(innovations),
            threads = threads(),
            notifications = notifications(),
            needs = needs(),
        )
    }

    private fun SeedInnovation.toDto() =
        InnovationDto(
            id = slug,
            title = title,
            summary = summary,
            description = description,
            areas = areas,
            targetGroups = targetGroups,
            stage = stage,
            region = region,
            mediaUrls = mediaUrls,
            averageRating = null,
            ratingsCount = 0,
            innovativeness = innovativeness,
            problemDiagnosis = problemDiagnosis,
            audienceDescription = audienceDescription,
            expectedChange = expectedChange,
            futureVision = futureVision,
        )

    private fun SeedChallenge.toDto() = ChallengeDto(slug, title, description, area, municipalities)

    private fun SeedMaterial.toDto() = MaterialDto(slug, title, description, type, url, areas)

    private fun titleOf(
        innovations: List<InnovationDto>,
        id: String,
    ): String = innovations.firstOrNull { it.id == id }?.title ?: id

    private class Rating(
        val innovationId: String,
        val user: String,
        val rating: Int,
        val comment: String?,
        val suggestion: String? = null,
        val daysAgo: Int,
    )

    private fun feedback(innovations: List<InnovationDto>): List<AdminFeedbackDto> =
        listOf(
            Rating(
                "sasiad-dla-seniora",
                "u-anna",
                5,
                "Model, który u nas ruszył w trzy miesiące. Polecam gminom.",
                null,
                40,
            ),
            Rating(
                "sasiad-dla-seniora",
                "u-piotr",
                4,
                "Dobry pomysł, wymaga stałego koordynatora.",
                "Warto dodać wzór umowy z wolontariuszem.",
                22,
            ),
            Rating(
                "cyfrowy-przewodnik-pokolen",
                "u-ewa",
                5,
                "Seniorzy uczą się chętniej od młodych, a młodzież dostaje poczucie sensu.",
                null,
                35,
            ),
            Rating("cyfrowy-przewodnik-pokolen", "u-marek", 4, null, null, 19),
            Rating(
                "bezpieczna-przystan-mlodziezy",
                "u-kasia",
                5,
                "Szkolenie z pierwszej pomocy psychologicznej robi różnicę.",
                "Dobrze byłoby przygotować wersję dla szkół wiejskich.",
                28,
            ),
            Rating(
                "wytchnieniowa-przystan-opiekuna",
                "u-tomek",
                4,
                "Opiekunowie wreszcie mają kilka godzin dla siebie.",
                null,
                14,
            ),
            Rating("mobilny-punkt-rehabilitacji-wiejskiej", "u-anna", 4, null, null, 30),
            Rating(
                "mobilny-punkt-rehabilitacji-wiejskiej",
                "u-piotr",
                3,
                "Zasięg zależy od liczby fizjoterapeutów.",
                "Rozważyć współpracę z uczelnią medyczną.",
                11,
            ),
            Rating(
                "mobilny-punkt-naprawczy-repair-cafe",
                "u-ewa",
                5,
                "Świetna integracja sąsiedzka przy okazji naprawy żelazka.",
                null,
                9,
            ),
            Rating(
                "wirtualna-przychodnia-dla-seniora",
                "u-marek",
                3,
                "Przycisk SOS wymaga lepszego opisu dla osób niedowidzących.",
                null,
                6,
            ),
            Rating(
                "kolorowe-ogrody-spolecznosciowe",
                "u-kasia",
                5,
                "Ogród stał się miejscem spotkań całego osiedla.",
                null,
                17,
            ),
            Rating(
                "punkt-doradztwa-energetycznego-dla-ubogich",
                "u-tomek",
                4,
                "Konkretne wskazówki, które realnie obniżyły rachunki.",
                null,
                4,
            ),
        ).mapIndexed { index, r ->
            AdminFeedbackDto(
                id = "feedback-seed-$index",
                innovationId = r.innovationId,
                innovationTitle = titleOf(innovations, r.innovationId),
                userId = r.user,
                rating = r.rating,
                comment = r.comment,
                suggestion = r.suggestion,
                createdAt = isoDaysFromNow(-r.daysAgo),
                updatedAt = isoDaysFromNow(-r.daysAgo),
            )
        }

    private fun testRequests(innovations: List<InnovationDto>): List<AdminTestRequestDto> =
        listOf(
            Triple(
                "sasiad-dla-seniora",
                "Gmina Wieliczka chce uruchomić pilotaż w dwóch sołectwach." to TestRequestStatus.NEW,
                3,
            ),
            Triple(
                "cyfrowy-przewodnik-pokolen",
                "Stowarzyszenie z Gorlic planuje zajęcia w bibliotece." to TestRequestStatus.NEW,
                2,
            ),
            Triple(
                "bezpieczna-przystan-mlodziezy",
                "Poradnia psychologiczna w Nowym Targu zgłasza gotowość do testów." to TestRequestStatus.NEW,
                1,
            ),
            Triple(
                "wytchnieniowa-przystan-opiekuna",
                "Centrum Usług Społecznych z Chrzanowa." to TestRequestStatus.ACCEPTED,
                12,
            ),
        ).mapIndexed { index, (innovationId, noteAndStatus, daysAgo) ->
            AdminTestRequestDto(
                id = "test-request-seed-$index",
                innovationId = innovationId,
                innovationTitle = titleOf(innovations, innovationId),
                userId = "u-organizacja-$index",
                note = noteAndStatus.first,
                status = noteAndStatus.second,
                createdAt = isoDaysFromNow(-daysAgo),
                updatedAt = isoDaysFromNow(-daysAgo),
            )
        }

    private fun idea(
        index: Int,
        title: String,
        essence: String,
        groups: List<TargetGroup>,
        stage: InnovationStage,
        status: IdeaStatus,
        daysAgo: Int,
        own: Boolean,
        comment: String? = null,
    ) = MockIdea(
        idea =
            IdeaDto(
                id = "idea-seed-$index",
                title = title,
                essence = essence,
                targetGroups = groups,
                stage = stage,
                status = status,
                adminComment = comment,
                createdAt = isoDaysFromNow(-daysAgo),
            ),
        authorId = if (own) DEMO_USER_ID else "u-autor-$index",
    )

    private fun ideas(): List<MockIdea> =
        listOf(
            idea(
                1,
                "Klub Sąsiedzkiej Pomocy w Bibliotece",
                "Filia biblioteki jako punkt, w którym seniorzy i sąsiedzi umawiają drobną pomoc: zakupy, naprawy, rozmowy.",
                listOf(TargetGroup.SENIORS, TargetGroup.RESIDENTS),
                InnovationStage.IDEA,
                IdeaStatus.SUBMITTED,
                3,
                own = true,
            ),
            idea(
                2,
                "Bank Narzędzi dla Seniorów",
                "Wypożyczalnia narzędzi i drobnego sprzętu z dowozem i pomocą wolontariusza przy pierwszym użyciu.",
                listOf(TargetGroup.SENIORS),
                InnovationStage.PILOT,
                IdeaStatus.ACCEPTED,
                21,
                own = true,
                comment = "Pomysł trafił do bazy inspiracji. Zachęcamy do złożenia wniosku w trwającym naborze.",
            ),
            idea(
                3,
                "Mobilna Świetlica dla Dzieci ze Wsi",
                "Bus z animatorami, który dojeżdża do sołectw bez świetlicy i prowadzi zajęcia popołudniowe.",
                listOf(TargetGroup.YOUTH, TargetGroup.FAMILIES),
                InnovationStage.IDEA,
                IdeaStatus.REJECTED,
                35,
                own = true,
                comment =
                    "W bazie jest już podobne rozwiązanie: „Cyfrowa Iskra”. " +
                        "Rozważ jego adaptację w Twojej gminie.",
            ),
            idea(
                4,
                "Mobilny Asystent Seniora",
                "Aplikacja łącząca wolontariuszy z seniorami potrzebującymi wsparcia w codziennych sprawach.",
                listOf(TargetGroup.SENIORS),
                InnovationStage.PILOT,
                IdeaStatus.SUBMITTED,
                2,
                own = false,
            ),
            idea(
                5,
                "Centrum Równych Szans",
                "Klub rówieśniczy wspierający młodzież z obszarów wiejskich w rozwijaniu pasji i integracji.",
                listOf(TargetGroup.YOUTH),
                InnovationStage.IDEA,
                IdeaStatus.IN_REVIEW,
                8,
                own = false,
            ),
            idea(
                6,
                "Podhalańskie Warsztaty Cyfrowe",
                "Wyjazdowe warsztaty obsługi e-urzędu i bankowości dla mieszkańców wsi górskich.",
                listOf(TargetGroup.RESIDENTS, TargetGroup.SENIORS),
                InnovationStage.IDEA,
                IdeaStatus.SUBMITTED,
                1,
                own = false,
            ),
        )

    private fun calls(): List<GrantCallDto> =
        listOf(
            GrantCallDto(
                id = INBOX_CALL,
                title = "Małopolski Inkubator Innowacji Społecznych – Nabór 2026",
                description = "Granty na rozwój i testowanie nowatorskich mikroinnowacji społecznych w Małopolsce.",
                opensAt = isoDaysFromNow(-14),
                closesAt = isoDaysFromNow(45),
                status = CallStatus.OPEN,
                fields =
                    listOf(
                        CallField("opis_problemu", "Opis problemu społecznego", true),
                        CallField("grupa_docelowa", "Odbiorcy innowacji", true),
                        CallField("budzet", "Szacowany budżet (PLN)", true),
                        CallField("partnerzy", "Potencjalni partnerzy", false),
                    ),
            ),
            GrantCallDto(
                id = SENIORS_CALL,
                title = "Wsparcie Samodzielności Seniorów – Edycja Wiosna 2027",
                description = "Inicjatywy wspierające aktywność i niezależność seniorów w środowisku lokalnym.",
                opensAt = isoDaysFromNow(30),
                closesAt = isoDaysFromNow(90),
                status = CallStatus.UPCOMING,
                fields =
                    listOf(
                        CallField("tytul", "Tytuł projektu", true),
                        CallField("zalozenia", "Główne założenia usługi opiekuńczej", true),
                    ),
            ),
            GrantCallDto(
                id = CLOSED_CALL,
                title = "Pilotaż Innowacji Włączających – 2025",
                description =
                    "Zakończony nabór pilotażowy projektów włączenia społecznego " +
                        "osób z niepełnosprawnościami.",
                opensAt = isoDaysFromNow(-180),
                closesAt = isoDaysFromNow(-30),
                status = CallStatus.CLOSED,
                fields = listOf(CallField("podsumowanie", "Podsumowanie rezultatów", true)),
            ),
        )

    private fun applications(): List<ApplicationDto> =
        listOf(
            ApplicationDto(
                id = "wniosek-seed-1",
                callId = INBOX_CALL,
                applicantId = "u-maria",
                status = ApplicationStatus.SUBMITTED,
                title = "Sąsiedzka Wymiana Usług",
                applicant =
                    IndividualApplicantDto(
                        firstName = "Maria",
                        lastName = "Przykładowa",
                        address = AddressDto("ul. Przykładowa", "12", "3", "30-001", "Kraków"),
                        phone = "500 000 000",
                        email = "maria.przykladowa@example.com",
                    ),
                description =
                    "Lokalna platforma, na której mieszkańcy osiedla wymieniają " +
                        "się usługami zamiast pieniędzy.",
                innovativeness = "Wymiana oparta na czasie, a nie na pieniądzu, z koordynatorem osiedlowym.",
                problemDiagnosis =
                    "Mieszkańcy osiedla nie znają się, a drobne usługi " +
                        "sąsiedzkie przestały się pojawiać.",
                socialArea = SocialArea.LONELINESS,
                audienceDescription = "Mieszkańcy osiedla wielorodzinnego, w tym seniorzy i młode rodziny.",
                expectedChange = "Więcej kontaktów sąsiedzkich i spadek poczucia osamotnienia.",
                futureVision = "Przeniesienie modelu na kolejne osiedla w dzielnicy.",
                plan =
                    ActionPlanDto(
                        preparation =
                            listOf(
                                PlanItemDto("Rekrutacja koordynatora i wolontariuszy", "2026-12", 300_000),
                            ),
                        testingPhase1 =
                            listOf(
                                PlanItemDto("Uruchomienie wymiany w dwóch klatkach", "2027-02", 600_000),
                            ),
                        testingPhase2 = listOf(PlanItemDto("Rozszerzenie na całe osiedle", "2027-05", 900_000)),
                    ),
                requestedGrantAmountGrosze = 1_800_000,
                projectTeam = "Maria Przykładowa i dwóch wolontariuszy.",
                declarations = RopsDeclarations.getRequiredIds(ApplicantType.INDIVIDUAL).toList(),
                submittedAt = isoDaysFromNow(-4),
                createdAt = isoDaysFromNow(-9),
                updatedAt = isoDaysFromNow(-4),
            ),
        )

    private val pilotSteps =
        listOf(
            AdaptationStepDto(
                "Diagnoza lokalnych potrzeb",
                "Rozmowy z mieszkańcami i partnerami, wybór pierwszej lokalizacji.",
                0,
            ),
            AdaptationStepDto(
                "Zespół i szkolenie",
                "Rekrutacja koordynatora oraz wolontariuszy, szkolenie z zasad bezpieczeństwa.",
                1,
            ),
            AdaptationStepDto(
                "Start pilotażu",
                "Uruchomienie usługi w jednej lokalizacji i zbieranie opinii uczestników.",
                2,
            ),
            AdaptationStepDto("Monitoring i korekty", "Cotygodniowy przegląd wskaźników, poprawki organizacyjne.", 4),
            AdaptationStepDto("Ewaluacja", "Podsumowanie wyników i decyzja o skalowaniu na kolejne miejsca.", 6),
        )

    private fun adaptation(
        id: String,
        innovations: List<InnovationDto>,
        innovationId: String,
        institution: InstitutionProfile,
        status: AdaptationStatus,
        own: Boolean,
        comment: String? = null,
    ) = MockAdaptation(
        adaptation =
            AdaptationDto(
                id = id,
                innovationId = innovationId,
                innovationTitle = titleOf(innovations, innovationId),
                institution = institution,
                plan =
                    AdaptationPlanDto(
                        serviceForm =
                            "Punkt usługi prowadzony przez ${institution.staffCount} " +
                                "osoby z udziałem wolontariuszy.",
                        steps = pilotSteps,
                        requiredResources =
                            listOf(
                                "Lokal na spotkania",
                                "Koordynator na część etatu",
                                "Wolontariusze",
                                "Materiały informacyjne",
                            ),
                        risks = listOf("Zbyt mała liczba wolontariuszy", "Niska znajomość usługi wśród mieszkańców"),
                        estimatedCostPln = estimatePilotCostPln(institution.staffCount),
                        exceedsBudget = estimatePilotCostPln(institution.staffCount) > institution.budgetPln,
                    ),
                model = DEMO_MODEL_NAME,
                status = status,
                adminComment = comment,
                createdAt = isoDaysFromNow(if (own) -10 else -2),
            ),
        authorId = if (own) DEMO_USER_ID else "u-jst",
    )

    private fun adaptations(innovations: List<InnovationDto>): List<MockAdaptation> =
        listOf(
            adaptation(
                "plan-seed-1",
                innovations,
                "cyfrowy-przewodnik-pokolen",
                InstitutionProfile(
                    InstitutionType.NGO,
                    2,
                    25_000,
                    "Stowarzyszenie w małym mieście, mamy salę w domu kultury.",
                ),
                AdaptationStatus.APPROVED,
                own = true,
                comment = "Plan jest realny. Zachęcamy do kontaktu z autorami innowacji.",
            ),
            adaptation(
                "plan-seed-2",
                innovations,
                "sasiad-dla-seniora",
                InstitutionProfile(
                    InstitutionType.LOCAL_GOVERNMENT,
                    3,
                    40_000,
                    "Gmina wiejska, rozproszona zabudowa, wielu samotnych seniorów.",
                ),
                AdaptationStatus.PENDING_REVIEW,
                own = false,
            ),
        )

    private fun message(
        id: String,
        author: String,
        role: ParticipantRole,
        text: String,
        daysAgo: Int,
    ) = MessageDto(id, author, role, text, isoDaysFromNow(-daysAgo))

    private fun threads(): List<MockThread> =
        listOf(
            MockThread(
                ThreadDto(
                    "watek-seed-1",
                    "Termin składania wniosków w naborze 2026",
                    null,
                    isoDaysFromNow(-1),
                    unread = true,
                ),
                listOf(
                    message(
                        "msg-seed-1",
                        DEMO_AUTHOR_NAME,
                        ParticipantRole.AUTHOR,
                        "Dzień dobry, czy wniosek można uzupełniać po złożeniu?",
                        3,
                    ),
                    message(
                        "msg-seed-2",
                        ROPS_TEAM_NAME,
                        ParticipantRole.ADMIN,
                        "Dzień dobry. Do końca naboru wniosek można zapisywać jako wersję roboczą, po złożeniu nie da się go już zmienić.",
                        1,
                    ),
                ),
            ),
            MockThread(
                ThreadDto(
                    "watek-seed-2",
                    "Konsultacja pomysłu: Bank Narzędzi dla Seniorów",
                    "idea-seed-2",
                    isoDaysFromNow(-6),
                    unread = false,
                ),
                listOf(
                    message(
                        "msg-seed-3",
                        ROPS_TEAM_NAME,
                        ParticipantRole.ADMIN,
                        "Pomysł zaakceptowany. Chętnie omówimy partnerów lokalnych.",
                        7,
                    ),
                    message(
                        "msg-seed-4",
                        DEMO_AUTHOR_NAME,
                        ParticipantRole.AUTHOR,
                        "Dziękuję! Mam już kontakt z biblioteką w dzielnicy.",
                        6,
                    ),
                ),
            ),
        )

    private fun notifications(): List<NotificationDto> =
        listOf(
            NotificationDto(
                "powiadomienie-seed-1",
                NotificationType.CALL_PUBLISHED,
                "Nowy nabór",
                "Ruszył nabór „Małopolski Inkubator Innowacji Społecznych – 2026”.",
                isoDaysFromNow(-14),
                false,
            ),
            NotificationDto(
                "powiadomienie-seed-2",
                NotificationType.MESSAGE_RECEIVED,
                "Nowa wiadomość",
                "Zespół Hubu odpowiedział w wątku o terminie składania wniosków.",
                isoDaysFromNow(-1),
                false,
            ),
            NotificationDto(
                "powiadomienie-seed-3",
                NotificationType.IDEA_STATUS_CHANGED,
                "Pomysł zaakceptowany",
                "Pomysł „Bank Narzędzi dla Seniorów” został zaakceptowany.",
                isoDaysFromNow(-21),
                true,
            ),
            NotificationDto(
                "powiadomienie-seed-4",
                NotificationType.IDEA_SUBMITTED,
                "Pomysł zgłoszony",
                "Pomysł „Klub Sąsiedzkiej Pomocy w Bibliotece” czeka na przegląd.",
                isoDaysFromNow(-3),
                true,
            ),
        )

    private fun need(
        index: Int,
        description: String,
        municipality: String?,
        area: SocialArea?,
        matched: Boolean,
        daysAgo: Int,
    ) = MockNeed("potrzeba-seed-$index", description, municipality, area, matched, null, isoDaysFromNow(-daysAgo))

    private fun needs(): List<MockNeed> =
        listOf(
            need(
                1,
                "Samotni seniorzy w naszej gminie nie mają jak dojechać do przychodni.",
                "Wieliczka",
                SocialArea.AGING,
                true,
                40,
            ),
            need(
                2,
                "Młodzież po pandemii ma problemy psychiczne, a do psychologa czeka się miesiącami.",
                "Kraków",
                SocialArea.MENTAL_HEALTH,
                true,
                33,
            ),
            need(
                3,
                "Starsi mieszkańcy wsi nie radzą sobie z e-urzędem i bankowością internetową.",
                "Gorlice",
                SocialArea.DIGITAL_EXCLUSION,
                true,
                27,
            ),
            need(
                4,
                "Opiekunowie osób leżących nie mają kiedy odpocząć, potrzebna opieka na kilka godzin.",
                "Chrzanów",
                SocialArea.SERVICE_ACCESS,
                true,
                20,
            ),
            need(
                5,
                "Brakuje miejsca spotkań dla młodzieży po szkole, młodzi wyjeżdżają.",
                "Miechów",
                SocialArea.DEPOPULATION,
                true,
                15,
            ),
            need(
                6,
                "Potrzebujemy tłumacza migowego w urzędzie w weekendy.",
                "Nowy Targ",
                SocialArea.SERVICE_ACCESS,
                false,
                6,
            ),
            need(
                7,
                "Opieka wytchnieniowa w nocy dla rodzin z dziećmi z autyzmem.",
                "Tarnów",
                SocialArea.COORDINATION,
                false,
                4,
            ),
            need(
                8,
                "Jak pomóc osobom w kryzysie bezdomności zimą w małych miastach?",
                "Oświęcim",
                SocialArea.OTHER,
                false,
                2,
            ),
        )
}
