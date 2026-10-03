package io.github.mfabisiak.hubmi.domain

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import kotlin.test.*

class DraftsTest {
    private fun innovation(
        title: String = "Klub Sąsiedzki",
        mediaUrls: List<String> = emptyList(),
        region: String? = null,
        areas: List<SocialArea> = listOf(SocialArea.AGING),
    ) = UpsertInnovationRequest(
        title = title,
        summary = "Krótkie podsumowanie",
        description = "Dłuższy opis innowacji",
        areas = areas,
        targetGroups = listOf(TargetGroup.SENIORS),
        stage = InnovationStage.PILOT,
        region = region,
        mediaUrls = mediaUrls,
    )

    private fun errorsOf(result: Either<arrow.core.NonEmptyList<FieldError>, *>): List<FieldError> =
        assertIs<Either.Left<arrow.core.NonEmptyList<FieldError>>>(result).value

    @Test
    fun innovationDraftTrimsAndDeduplicates() {
        val draft =
            assertIs<Either.Right<InnovationDraft>>(
                InnovationDraft.parse(
                    innovation(
                        title = "  Klub Sąsiedzki  ",
                        mediaUrls = listOf(" https://example.com/a "),
                        region = " Małopolska ",
                        areas = listOf(SocialArea.AGING, SocialArea.AGING),
                    ),
                ),
            ).value

        assertEquals("Klub Sąsiedzki", draft.title.value)
        assertEquals("https://example.com/a", draft.mediaUrls.single().value)
        assertEquals("Małopolska", draft.region?.value)
        assertEquals(listOf(SocialArea.AGING), draft.areas.toList())
    }

    @Test
    fun innovationDraftReportsEveryInvalidField() {
        val errors =
            errorsOf(
                InnovationDraft.parse(
                    innovation(
                        title = "AB",
                        mediaUrls = listOf("https://ok.example", "not-a-url", "ftp://example.com"),
                        areas = emptyList(),
                    ),
                ),
            )

        assertEquals(setOf("title", "areas", "mediaUrls[1]", "mediaUrls[2]"), errors.map { it.field }.toSet())
        assertEquals(FieldErrorCode.Range(3, 120), errors.single { it.field == "title" }.code)
        assertEquals(FieldErrorCode.Required, errors.single { it.field == "areas" }.code)
        assertEquals(FieldErrorCode.InvalidFormat, errors.single { it.field == "mediaUrls[1]" }.code)
    }

    @Test
    fun blankRegionIsRejectedButAbsentRegionIsNot() {
        assertIs<Either.Right<InnovationDraft>>(InnovationDraft.parse(innovation(region = null)))
        assertEquals(listOf("region"), errorsOf(InnovationDraft.parse(innovation(region = "  "))).map { it.field })
    }

    @Test
    fun challengeDraftNamesTheOffendingMunicipality() {
        val errors =
            errorsOf(
                ChallengeDraft.parse(
                    UpsertChallengeRequest(
                        title = "Wyzwanie",
                        description = "Opis",
                        area = SocialArea.LONELINESS,
                        municipalities = listOf("Kraków", " "),
                    ),
                ),
            )

        assertEquals(listOf("municipalities[1]"), errors.map { it.field })
        assertEquals(FieldErrorCode.Blank, errors.single().code)
    }

    @Test
    fun materialDraftRequiresHttpUrlAndAreas() {
        val errors =
            errorsOf(
                MaterialDraft.parse(
                    UpsertMaterialRequest(
                        title = "Poradnik",
                        description = "Opis",
                        type = MaterialType.GUIDE,
                        url = "https:// spacja.pl",
                        areas = emptyList(),
                    ),
                ),
            )

        assertEquals(setOf("url", "areas"), errors.map { it.field }.toSet())
    }

    @Test
    fun httpUrlRequiresSchemeAndHost() {
        assertIs<Either.Right<HttpUrl>>(HttpUrl.parse("http://example.com/path?q=1", "url"))
        listOf("http://", "example.com", "mailto:a@b.pl", "javascript:alert(1)").forEach {
            assertIs<Either.Left<FieldError>>(HttpUrl.parse(it, "url"), "'$it' should be rejected")
        }
    }

    @Test
    fun searchQueryBlankMeansNoFilterAndTooLongIsRejected() {
        assertEquals(Either.Right(null), SearchQuery.parse("   "))
        assertEquals(Either.Right(null), SearchQuery.parse(null))
        assertEquals("senior", assertIs<Either.Right<SearchQuery?>>(SearchQuery.parse(" senior ")).value?.value)
        assertIs<Either.Left<*>>(SearchQuery.parse("x".repeat(101)))
    }

    @Test
    fun idsRejectNonObjectIds() {
        assertIs<Either.Right<InnovationId>>(InnovationId.parse("66a1f0c2e4b0a1b2c3d4e5f6"))
        assertIs<Either.Left<*>>(ChallengeId.parse("abc"))
        assertIs<Either.Left<*>>(MaterialId.parse(""))
    }
}
