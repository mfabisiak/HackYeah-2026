package io.github.mfabisiak.hubmi.challenges

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.UpsertChallengeRequest
import io.github.mfabisiak.hubmi.common.errorsOf
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
