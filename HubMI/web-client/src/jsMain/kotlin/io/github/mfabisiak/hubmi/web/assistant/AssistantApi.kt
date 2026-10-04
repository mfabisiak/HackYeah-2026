package io.github.mfabisiak.hubmi.web.assistant

import io.github.mfabisiak.hubmi.api.AssistMode
import io.github.mfabisiak.hubmi.web.StreamJs
import io.github.mfabisiak.hubmi.web.ideas.CreateIdeaJs

/**
 * The idea creator's assistant (`/api/ideas/assist`, `/api/ideas/{id}/assist`); all calls need login.
 *
 * Every call returns a [StreamJs] at once. Its `result` promise resolves to the whole answer, so a caller that only
 * wants that ignores the listener; one that wants to show suggestions as they are written passes an
 * [AssistListenerJs] with `onSimilar`, `onSuggestion` and `onFlow`. The model's part can fail without an error: check
 * `aiStatus` of the answer.
 */
@JsExport
interface AssistantApi {
    /** For an idea that is not saved yet, as typed in the form. @param mode an `AssistMode` name. */
    fun assistDraft(
        idea: CreateIdeaJs,
        mode: String,
        listener: AssistListenerJs? = null,
    ): StreamJs<AssistResponseJs>

    /** For a saved idea of the caller. @param mode an `AssistMode` name. */
    fun assistIdea(
        ideaId: String,
        mode: String,
        listener: AssistListenerJs? = null,
    ): StreamJs<AssistResponseJs>
}
