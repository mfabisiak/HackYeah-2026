package io.github.mfabisiak.hubmi.materials

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
import io.github.mfabisiak.hubmi.common.errorsOf
import io.github.mfabisiak.hubmi.innovations.InnovationDraft
import io.github.mfabisiak.hubmi.innovations.InnovationId
import kotlin.test.*

class MaterialDraftTest {
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
    fun materialIdRejectsNonObjectId() {
        assertIs<Either.Left<*>>(MaterialId.parse(""))
    }
}
