package io.github.mfabisiak.hubmi.matching

import arrow.core.Either

interface EmbeddingClient {
    /** One embedding per text, in the order of [texts]. */
    suspend fun embed(texts: List<String>): Either<EmbeddingError, List<Embedding>>
}
