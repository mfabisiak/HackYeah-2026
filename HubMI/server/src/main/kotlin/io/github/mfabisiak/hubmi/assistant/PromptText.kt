package io.github.mfabisiak.hubmi.assistant

/** Makes the user's words safe to put between the tags of a prompt. */
internal object PromptText {
    /** Removes the angle brackets, so that the user cannot close the tag the text is in and write instructions after it. */
    fun of(text: String): String = text.replace('<', ' ').replace('>', ' ')
}
