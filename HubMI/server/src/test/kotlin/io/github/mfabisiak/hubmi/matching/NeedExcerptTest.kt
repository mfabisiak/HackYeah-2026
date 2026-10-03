package io.github.mfabisiak.hubmi.matching

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NeedExcerptTest {
    @Test
    fun shortTextIsReturnedUnchanged() {
        assertEquals("Mama mieszka sama na wsi", excerptOf("Mama mieszka sama na wsi"))
    }

    @Test
    fun longTextIsCutAtAWordBoundaryWithAnEllipsis() {
        val excerpt = excerptOf("Mama mieszka sama na wsi, ".repeat(10))

        assertTrue(excerpt.length <= 121, "excerpt has ${excerpt.length} chars")
        assertTrue(
            excerpt.endsWith("wsi…") || excerpt.endsWith("na…") || excerpt.endsWith("sama…") ||
                excerpt.endsWith("mieszka…"),
        )
    }

    @Test
    fun textWithoutSpacesIsCutHard() {
        assertEquals("a".repeat(120) + "…", excerptOf("a".repeat(300)))
    }
}
