package io.github.mfabisiak.hubmi.common

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
import io.github.mfabisiak.hubmi.challenges.ChallengeDraft
import io.github.mfabisiak.hubmi.challenges.ChallengeId
import io.github.mfabisiak.hubmi.innovations.InnovationDraft
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.github.mfabisiak.hubmi.materials.MaterialDraft
import io.github.mfabisiak.hubmi.materials.MaterialId
import kotlin.test.*

class ParsingTest {
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
}
