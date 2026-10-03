package io.github.mfabisiak.hubmi.matching

import kotlin.math.ln
import kotlin.math.min

/** Stems of one field of a document and the weight of that field in the relevance score. */
data class WeightedStems(
    val stems: List<String>,
    val weight: Double,
)

/** A document handed to [Bm25Index.build]; [key] is returned in the hits. */
data class IndexInput<K>(
    val key: K,
    val fields: List<WeightedStems>,
)

/**
 * @property score relevance in `0..1`: the BM25 score divided by the best score a document could get for this query
 * (every query term maximally frequent), so queries of different length are comparable. Query terms unknown to the
 * index count as maximally informative, which pulls queries full of foreign words below the threshold.
 */
data class Bm25Hit<K>(
    val key: K,
    val score: Double,
    val matchedStems: Set<String>,
)

/** In-memory BM25 over weighted fields (a field of weight w counts as w occurrences of each of its terms). */
class Bm25Index<K> private constructor(
    private val documents: List<IndexedDocument<K>>,
    private val documentFrequency: Map<String, Int>,
    private val averageLength: Double,
    private val k1: Double,
    private val b: Double,
) {
    private class IndexedDocument<K>(
        val key: K,
        val termWeights: Map<String, Double>,
        val length: Double,
    )

    fun search(queryStems: Collection<String>): List<Bm25Hit<K>> {
        val terms = queryStems.distinct()
        val idf = terms.associateWith(::idf)
        val bound = idf.values.sum() * (k1 + 1)
        return when {
            documents.isEmpty() || bound <= 0.0 -> {
                emptyList()
            }

            else -> {
                documents
                    .map { document -> hit(document, terms, idf, bound) }
                    .filter { it.matchedStems.isNotEmpty() }
                    .sortedByDescending { it.score }
            }
        }
    }

    private fun hit(
        document: IndexedDocument<K>,
        terms: List<String>,
        idf: Map<String, Double>,
        bound: Double,
    ): Bm25Hit<K> {
        val lengthNorm = k1 * (1 - b + b * document.length / averageLength)
        val matched = terms.filter { document.termWeights.getOrDefault(it, 0.0) > 0.0 }
        val raw =
            matched.sumOf { term ->
                val tf = document.termWeights.getValue(term)
                idf.getValue(term) * tf * (k1 + 1) / (tf + lengthNorm)
            }
        return Bm25Hit(document.key, min(1.0, raw / bound), matched.toSet())
    }

    private fun idf(term: String): Double {
        val df = documentFrequency.getOrDefault(term, 0)
        return ln(1 + (documents.size - df + 0.5) / (df + 0.5))
    }

    companion object {
        const val DEFAULT_K1 = 1.2
        const val DEFAULT_B = 0.75

        fun <K> build(
            inputs: List<IndexInput<K>>,
            k1: Double = DEFAULT_K1,
            b: Double = DEFAULT_B,
        ): Bm25Index<K> {
            val documents =
                inputs.map { input ->
                    IndexedDocument(
                        key = input.key,
                        termWeights =
                            input.fields
                                .filter { it.weight > 0.0 }
                                .flatMap { field -> field.stems.map { it to field.weight } }
                                .groupBy({ it.first }, { it.second })
                                .mapValues { (_, weights) -> weights.sum() },
                        length = input.fields.sumOf { it.stems.size * it.weight },
                    )
                }
            return Bm25Index(
                documents = documents,
                documentFrequency =
                    documents
                        .flatMap { it.termWeights.keys }
                        .groupingBy { it }
                        .eachCount(),
                averageLength = documents.map { it.length }.average().takeIf { !it.isNaN() } ?: 1.0,
                k1 = k1,
                b = b,
            )
        }
    }
}
