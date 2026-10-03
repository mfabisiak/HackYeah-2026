package io.github.mfabisiak.hubmi.materials

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.MaterialType
import io.github.mfabisiak.hubmi.api.UpsertMaterialRequest
import io.github.mfabisiak.hubmi.common.errorsOf
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
