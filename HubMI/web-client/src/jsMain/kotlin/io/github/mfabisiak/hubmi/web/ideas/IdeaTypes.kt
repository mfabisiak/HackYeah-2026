package io.github.mfabisiak.hubmi.web.ideas

import arrow.core.Either
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumsOf
import io.github.mfabisiak.hubmi.web.names

@JsExport
class CreateIdeaJs(
    val title: String,
    /** What the idea is about, in a nutshell. */
    val essence: String,
    /** `TargetGroup` names. */
    val targetGroups: Array<String>,
    /** `InnovationStage` name. */
    val stage: String,
)

@JsExport
class IdeaJs(
    val id: String,
    val title: String,
    val essence: String,
    val targetGroups: Array<String>,
    val stage: String,
    /** `IdeaStatus` name. */
    val status: String,
    val adminComment: String?,
    val createdAt: String,
)

@JsExport
class CallFieldJs(
    val key: String,
    val label: String,
    val required: Boolean,
)

@JsExport
class GrantCallJs(
    val id: String,
    val title: String,
    val description: String,
    val opensAt: String,
    val closesAt: String,
    /** `CallStatus` name. */
    val status: String,
    /** Form template the application generator renders for this call. */
    val fields: Array<CallFieldJs>,
)

@JsExport
class UpsertCallJs(
    val title: String,
    val description: String,
    val opensAt: String,
    val closesAt: String,
    val fields: Array<CallFieldJs>,
)

/** One answer of an application form; `Map` is not JS-friendly, so answers travel as key/value pairs. */
@JsExport
class AnswerJs(
    val key: String,
    val value: String,
)

@JsExport
class ApplicationJs(
    val id: String,
    val callId: String,
    val ideaId: String?,
    val createdAt: String,
)

internal fun IdeaDto.toJs(): IdeaJs =
    IdeaJs(id, title, essence, targetGroups.names(), stage.name, status.name, adminComment, createdAt)

internal fun CreateIdeaJs.toDto(): Either<ApiErrorJs, CreateIdeaRequest> =
    either {
        CreateIdeaRequest(
            title = title,
            essence = essence,
            targetGroups = enumsOf<TargetGroup>(targetGroups, "targetGroups"),
            stage = enumOf<InnovationStage>(stage, "stage"),
        )
    }

internal fun GrantCallDto.toJs(): GrantCallJs =
    GrantCallJs(id, title, description, opensAt, closesAt, status.name, fields.map { it.toJs() }.toTypedArray())

internal fun ApplicationDto.toJs(): ApplicationJs = ApplicationJs(id, callId, ideaId, createdAt)

internal fun UpsertCallJs.toDto(): UpsertCallRequest =
    UpsertCallRequest(title, description, opensAt, closesAt, fields.map { it.toDto() })

internal fun List<AnswerJs>.toAnswers(): Map<String, String> = associate { it.key to it.value }

private fun CallField.toJs(): CallFieldJs = CallFieldJs(key, label, required)

private fun CallFieldJs.toDto(): CallField = CallField(key, label, required)
