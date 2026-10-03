package io.github.mfabisiak.hubmi.ideas

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.common.asNel
import io.github.mfabisiak.hubmi.common.toNonEmpty

data class IdeaDraft(
    val title: String,
    val essence: String,
    val targetGroups: NonEmptyList<TargetGroup>,
    val stage: InnovationStage,
) {
    companion object {
        private const val MAX_TITLE_LENGTH = 200
        private const val MAX_ESSENCE_LENGTH = 1000

        fun parse(request: CreateIdeaRequest): EitherNel<FieldError, IdeaDraft> =
            Either.zipOrAccumulate(
                validateTitle(request.title).asNel(),
                validateEssence(request.essence).asNel(),
                request.targetGroups
                    .toNonEmpty(
                        "targetGroups",
                        "Należy wybrać przynajmniej jedną grupę docelową",
                    ).asNel(),
            ) { title, essence, targetGroups ->
                IdeaDraft(
                    title = title,
                    essence = essence,
                    targetGroups = targetGroups,
                    stage = request.stage,
                )
            }

        private fun validateTitle(raw: String): Either<FieldError, String> =
            either {
                val trimmed = raw.trim()
                ensure(trimmed.isNotBlank()) {
                    FieldError("title", FieldErrorCode.Blank, "Tytuł pomysłu nie może być pusty")
                }
                ensure(trimmed.length <= MAX_TITLE_LENGTH) {
                    FieldError(
                        "title",
                        FieldErrorCode.InvalidFormat,
                        "Tytuł pomysłu nie może przekraczać $MAX_TITLE_LENGTH znaków",
                    )
                }
                trimmed
            }

        private fun validateEssence(raw: String): Either<FieldError, String> =
            either {
                val trimmed = raw.trim()
                ensure(trimmed.isNotBlank()) {
                    FieldError("essence", FieldErrorCode.Blank, "Istota pomysłu nie może być pusta")
                }
                ensure(trimmed.length <= MAX_ESSENCE_LENGTH) {
                    FieldError(
                        "essence",
                        FieldErrorCode.InvalidFormat,
                        "Istota pomysłu nie może przekraczać $MAX_ESSENCE_LENGTH znaków",
                    )
                }
                trimmed
            }
    }
}
