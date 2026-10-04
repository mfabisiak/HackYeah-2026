package io.github.mfabisiak.hubmi.assistant

/** Why a piece of the model's answer was not used. */
sealed interface ModelOutputError {
    /** Not JSON of the expected shape. */
    data class Malformed(
        val detail: String,
    ) : ModelOutputError

    data class BlankField(
        val field: String,
    ) : ModelOutputError

    data class TooLong(
        val field: String,
        val max: Int,
    ) : ModelOutputError

    /** Well-formed, but too little of it makes sense (a flow with fewer than two actors or steps left). */
    data class Unusable(
        val detail: String,
    ) : ModelOutputError
}
