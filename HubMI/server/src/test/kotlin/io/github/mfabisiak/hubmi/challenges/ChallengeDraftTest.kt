package io.github.mfabisiak.hubmi.challenges

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
import io.github.mfabisiak.hubmi.common.errorsOf
import io.github.mfabisiak.hubmi.innovations.InnovationDraft
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.github.mfabisiak.hubmi.materials.MaterialDraft
import io.github.mfabisiak.hubmi.materials.MaterialId
import kotlin.test.*

class ChallengeDraftTest {
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
    fun challengeIdRejectsNonObjectId() {
        assertIs<Either.Left<*>>(ChallengeId.parse("abc"))
    }
}
