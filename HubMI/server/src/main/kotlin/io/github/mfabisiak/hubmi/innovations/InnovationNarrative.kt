package io.github.mfabisiak.hubmi.innovations

import arrow.core.Either
import arrow.core.EitherNel
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.UpsertInnovationRequest
import io.github.mfabisiak.hubmi.common.asNel

/** Sections 4 to 8 of ROPS' application form; every one is optional because older library entries lack them. */
data class InnovationNarrative(
    val innovativeness: Narrative?,
    val problemDiagnosis: Narrative?,
    val audienceDescription: Narrative?,
    val expectedChange: Narrative?,
    val futureVision: Narrative?,
) {
    companion object {
        fun parse(request: UpsertInnovationRequest): EitherNel<FieldError, InnovationNarrative> =
            Either.zipOrAccumulate(
                Narrative.parse(request.innovativeness, "innovativeness").asNel(),
                Narrative.parse(request.problemDiagnosis, "problemDiagnosis").asNel(),
                Narrative.parse(request.audienceDescription, "audienceDescription").asNel(),
                Narrative.parse(request.expectedChange, "expectedChange").asNel(),
                Narrative.parse(request.futureVision, "futureVision").asNel(),
            ) { innovativeness, problemDiagnosis, audienceDescription, expectedChange, futureVision ->
                InnovationNarrative(
                    innovativeness = innovativeness,
                    problemDiagnosis = problemDiagnosis,
                    audienceDescription = audienceDescription,
                    expectedChange = expectedChange,
                    futureVision = futureVision,
                )
            }
    }
}
