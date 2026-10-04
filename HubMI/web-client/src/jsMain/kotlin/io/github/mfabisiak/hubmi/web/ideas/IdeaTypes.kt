package io.github.mfabisiak.hubmi.web.ideas

import arrow.core.Either
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.CallField
import io.github.mfabisiak.hubmi.api.CreateIdeaRequest
import io.github.mfabisiak.hubmi.api.DeclarationDto
import io.github.mfabisiak.hubmi.api.DeclarationsResponse
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.InnovationStage
import io.github.mfabisiak.hubmi.api.SaveApplicationDraftRequest
import io.github.mfabisiak.hubmi.api.TargetGroup
import io.github.mfabisiak.hubmi.api.UpsertCallRequest
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.enumOf
import io.github.mfabisiak.hubmi.web.enumsOf
import io.github.mfabisiak.hubmi.web.names
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

@JsExport
class ApplicationJs(
    val id: String,
    val callId: String,
    val applicantId: String,
    val ideaId: String?,
    val status: String,
    val formVersion: Int,
    val title: String?,
    val requestedGrantAmountGrosze: Int?,
    /**
     * Everything the applicant fills in (applicant variant, narratives, plan, declarations) as the JSON of
     * `SaveApplicationDraftRequest`, i.e. exactly what [ApplicationsApi.saveDraft] accepts back. The applicant is a
     * polymorphic type (`INDIVIDUAL`, `ENTITY`, `NON_FORMAL_GROUP`), which has no JS-friendly shape.
     */
    val contentJson: String,
    val submittedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@JsExport
class DeclarationJs(
    /** `DeclarationId` name. */
    val id: String,
    /** `DeclarationCategory` name. */
    val category: String,
    val text: String,
    val required: Boolean,
)

@JsExport
class DeclarationsJs(
    val formVersion: Int,
    val declarations: Array<DeclarationJs>,
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

internal fun ApplicationDto.toJs(): ApplicationJs =
    ApplicationJs(
        id = id,
        callId = callId,
        applicantId = applicantId,
        ideaId = ideaId,
        status = status.name,
        formVersion = formVersion,
        title = title,
        requestedGrantAmountGrosze = requestedGrantAmountGrosze,
        contentJson = contentJson(),
        submittedAt = submittedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun DeclarationsResponse.toJs(): DeclarationsJs =
    DeclarationsJs(formVersion, declarations.map { it.toJs() }.toTypedArray())

private fun DeclarationDto.toJs(): DeclarationJs = DeclarationJs(id.name, category.name, text, required)

/** Defaults are encoded too, so the TypeScript side always sees every key. */
private val contentJson = Json { encodeDefaults = true }

private fun ApplicationDto.contentJson(): String =
    contentJson.encodeToString(
        SaveApplicationDraftRequest(
            title = title,
            applicant = applicant,
            description = description,
            innovativeness = innovativeness,
            problemDiagnosis = problemDiagnosis,
            socialArea = socialArea,
            audienceDescription = audienceDescription,
            expectedChange = expectedChange,
            futureVision = futureVision,
            plan = plan,
            requestedGrantAmountGrosze = requestedGrantAmountGrosze,
            projectTeam = projectTeam,
            declarations = declarations,
        ),
    )

internal fun UpsertCallJs.toDto(): UpsertCallRequest =
    UpsertCallRequest(title, description, opensAt, closesAt, fields.map { it.toDto() })

private fun CallField.toJs(): CallFieldJs =
    CallFieldJs(
        key = key,
        label = label,
        required = required,
    )

private fun CallFieldJs.toDto(): CallField =
    CallField(
        key = key,
        label = label,
        required = required,
    )
