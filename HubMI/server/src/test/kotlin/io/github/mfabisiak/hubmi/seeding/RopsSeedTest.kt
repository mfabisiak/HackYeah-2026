package io.github.mfabisiak.hubmi.seeding

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.innovations.InnovationDraft
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Guards the committed `seed/rops-innovations.json` (produced by `scripts/scrape_rops.py`) without a database. */
class RopsSeedTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun load(path: String): List<SeedInnovationItem> =
        json.decodeFromString(checkNotNull(javaClass.classLoader.getResourceAsStream(path)).reader().readText())

    private val rops = load("seed/rops-innovations.json")

    @Test
    fun containsTheWholeLibrary() {
        assertTrue(rops.size >= 100, "ROPS library should have 100+ innovations (got ${rops.size})")
    }

    @Test
    fun slugsAreUniqueAcrossAllInnovationSeeds() {
        val slugs = (rops + load("seed/innovations.json")).map(SeedInnovationItem::slug)
        assertEquals(slugs.size, slugs.toSet().size)
    }

    @Test
    fun everyItemPassesTheDomainValidationUsedByTheApi() {
        rops.forEach { item ->
            val request =
                UpsertInnovationRequest(
                    title = item.title,
                    summary = item.summary,
                    description = item.description,
                    areas = item.areas,
                    targetGroups = item.targetGroups,
                    stage = item.stage,
                    region = item.region,
                    mediaUrls = item.mediaUrls,
                    problemDiagnosis = item.problemDiagnosis,
                    audienceDescription = item.audienceDescription,
                    expectedChange = item.expectedChange,
                )
            val result = InnovationDraft.parse(request)
            assertTrue(result is Either.Right, "${item.slug} is invalid: ${result.leftOrNull()}")
        }
    }

    @Test
    fun everyItemLinksBackToItsRopsPage() {
        rops.forEach { item ->
            assertTrue(
                item.mediaUrls.firstOrNull()?.startsWith("https://rops.krakow.pl/") == true,
                "${item.slug}: first media URL should be the ROPS source page",
            )
        }
    }

    @Test
    fun itemsAreTestedInnovations() {
        assertTrue(rops.all { it.stage == InnovationStage.TESTED })
    }
}
