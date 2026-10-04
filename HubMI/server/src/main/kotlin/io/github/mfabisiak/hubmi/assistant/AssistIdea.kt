package io.github.mfabisiak.hubmi.assistant

import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.ideas.IdeaDraft
import io.github.mfabisiak.hubmi.matching.PersonalDataScrubber

/** The idea the assistant works on, whether typed in the form or saved, cleaned of personal data. */
data class AssistIdea(
    val title: String,
    val essence: String,
    val targetGroups: List<TargetGroup>,
    val stage: InnovationStage,
) {
    /** What the library is searched with. */
    fun query(): String = "$title. $essence"

    companion object {
        fun of(draft: IdeaDraft): AssistIdea =
            AssistIdea(
                title = PersonalDataScrubber.scrub(draft.title),
                essence = PersonalDataScrubber.scrub(draft.essence),
                targetGroups = draft.targetGroups.toList(),
                stage = draft.stage,
            )

        fun of(idea: IdeaDto): AssistIdea =
            AssistIdea(
                title = PersonalDataScrubber.scrub(idea.title),
                essence = PersonalDataScrubber.scrub(idea.essence),
                targetGroups = idea.targetGroups,
                stage = idea.stage,
            )
    }
}
