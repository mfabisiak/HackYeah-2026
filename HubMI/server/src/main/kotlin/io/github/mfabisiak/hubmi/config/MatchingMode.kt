package io.github.mfabisiak.hubmi.config

/** How `POST /api/matches` finds innovations (`MATCHING_MODE`, case-insensitive). */
enum class MatchingMode {
    /** BM25 over normalised text only; needs no models. */
    KEYWORD,

    /** BM25 plus embeddings from a local Ollama, falling back to [KEYWORD] while Ollama is unavailable. */
    HYBRID,
    ;

    companion object {
        fun fromEnv(value: String): MatchingMode? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
