package io.github.mfabisiak.hubmi.service.matching

import arrow.core.Either
import arrow.core.right
import io.github.mfabisiak.hubmi.models.InnovationItem
import io.github.mfabisiak.hubmi.service.DomainError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bson.types.ObjectId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * The innovation catalogue indexed for matching. It is built lazily, kept in memory, dropped by [invalidate] whenever
 * an innovation changes and rebuilt after [ttl] anyway (other instances or direct edits cannot call [invalidate]).
 */
class InnovationIndex(
    private val analyzer: TextAnalyzer,
    private val load: suspend () -> Either<DomainError, List<InnovationItem>>,
    private val weights: FieldWeights = FieldWeights(),
    private val ttl: Duration = DEFAULT_TTL,
    private val clock: Clock = Clock.systemUTC(),
) {
    class Snapshot(
        val index: Bm25Index<ObjectId>,
        val innovations: Map<ObjectId, InnovationItem>,
        val builtAt: Instant,
    )

    private val current = AtomicReference<Snapshot?>(null)
    private val rebuild = Mutex()

    suspend fun snapshot(): Either<DomainError, Snapshot> =
        fresh()?.right()
            ?: rebuild.withLock {
                fresh()?.right() ?: load().map(::build).onRight(current::set)
            }

    fun invalidate() = current.set(null)

    private fun fresh(): Snapshot? = current.get()?.takeIf { Duration.between(it.builtAt, clock.instant()) < ttl }

    private fun build(innovations: List<InnovationItem>): Snapshot =
        Snapshot(
            index = Bm25Index.build(innovations.map(::inputOf)),
            innovations = innovations.associateBy(InnovationItem::id),
            builtAt = clock.instant(),
        )

    private fun inputOf(item: InnovationItem): IndexInput<ObjectId> =
        IndexInput(
            key = item.id,
            fields =
                listOf(
                    WeightedStems(analyzer.stems(item.title), weights.title),
                    WeightedStems(item.keywords.flatMap(analyzer::stems), weights.keywords),
                    WeightedStems(analyzer.stems(item.summary), weights.summary),
                    WeightedStems(analyzer.stems(item.description), weights.description),
                    WeightedStems(stemsOf(item.problemDiagnosis), weights.problemDiagnosis),
                    WeightedStems(stemsOf(item.audienceDescription), weights.audienceDescription),
                    WeightedStems(stemsOf(item.expectedChange), weights.expectedChange),
                    WeightedStems(stemsOf(item.innovativeness), weights.innovativeness),
                    WeightedStems(stemsOf(item.futureVision), weights.futureVision),
                ),
        )

    private fun stemsOf(text: String?): List<String> = text?.let(analyzer::stems).orEmpty()

    private companion object {
        val DEFAULT_TTL: Duration = Duration.ofMinutes(5)
    }
}
