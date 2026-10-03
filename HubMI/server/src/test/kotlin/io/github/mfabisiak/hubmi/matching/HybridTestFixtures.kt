package io.github.mfabisiak.hubmi.matching

import arrow.core.Either
import arrow.core.right
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import java.util.concurrent.atomic.AtomicReference

/** A tiny catalogue where "dojazd do lekarza" and "transport" share no word, but share a concept. */
object HybridTestFixtures {
    val transport = innovation("Transport door-to-door", "Dowóz osób na wózkach do przychodni i pracy")
    val garden = innovation("Kolorowe ogrody", "Wspólna uprawa warzyw i kwiatów w mieście")
    val loneliness = innovation("Srebrna linia", "Rozmowy telefoniczne dla samotnych seniorów")

    private val concepts =
        listOf(
            setOf("transport", "dojazd", "dowóz", "dojechać"),
            setOf("lekarz", "lekarza", "przychodni", "chorego"),
            setOf("warzyw", "ogrod", "ogrody", "kwiatów"),
            setOf("samotn", "rozmow"),
        )

    fun embedder() = FakeEmbeddingClient(concepts)

    fun innovation(
        title: String,
        summary: String,
    ) = InnovationItem(
        title = title,
        summary = summary,
        description = summary,
        areas = listOf(SocialArea.AGING),
        targetGroups = listOf(TargetGroup.SENIORS),
        stage = InnovationStage.IMPLEMENTED,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    /** Catalogue that can be edited between calls, standing in for the repository. */
    class Catalogue(
        initial: List<InnovationItem>,
    ) {
        private val items = AtomicReference(initial)

        fun replace(innovations: List<InnovationItem>) = items.set(innovations)

        val load: suspend () -> Either<io.github.mfabisiak.hubmi.common.DomainError, List<InnovationItem>> =
            { items.get().right() }
    }

    class Engines(
        val embedder: FakeEmbeddingClient,
        val index: InnovationIndex,
        val keyword: KeywordMatchingEngine,
        val vectors: VectorIndex,
        val hybrid: HybridMatchingEngine,
    )

    fun engines(
        catalogue: Catalogue,
        embedder: FakeEmbeddingClient = embedder(),
        config: HybridMatchingEngine.Config = HybridMatchingEngine.Config(),
    ): Engines {
        val analyzer = TextAnalyzer(PrefixStemmer())
        val index = InnovationIndex(analyzer, load = catalogue.load)
        val keyword = KeywordMatchingEngine(index, analyzer)
        val vectors = VectorIndex(embedder)
        return Engines(
            embedder,
            index,
            keyword,
            vectors,
            HybridMatchingEngine(index, vectors, embedder, keyword, config),
        )
    }
}
