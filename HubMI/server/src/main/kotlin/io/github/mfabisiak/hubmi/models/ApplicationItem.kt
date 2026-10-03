package io.github.mfabisiak.hubmi.models

import io.github.mfabisiak.hubmi.api.ActionPlanDto
import io.github.mfabisiak.hubmi.api.ApplicantDto
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.SocialArea
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class ApplicationItem(
    @SerialName("_id")
    @Contextual
    val id: ObjectId = ObjectId(),
    @Contextual
    val callId: ObjectId,
    val applicantId: String,
    @Contextual
    val ideaId: ObjectId? = null,
    val status: ApplicationStatus = ApplicationStatus.DRAFT,
    val formVersion: Int = 1,
    val title: String? = null,
    val applicant: ApplicantDto? = null,
    val description: String? = null,
    val innovativeness: String? = null,
    val problemDiagnosis: String? = null,
    val socialArea: SocialArea? = null,
    val audienceDescription: String? = null,
    val expectedChange: String? = null,
    val futureVision: String? = null,
    val plan: ActionPlanDto? = null,
    val requestedGrantAmountGrosze: Int? = null,
    val projectTeam: String? = null,
    val declarations: List<String> = emptyList(),
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val createdAt: Instant,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val updatedAt: Instant,
)

fun ApplicationItem.toDto(): ApplicationDto =
    ApplicationDto(
        id = id.toHexString(),
        callId = callId.toHexString(),
        applicantId = applicantId,
        ideaId = ideaId?.toHexString(),
        status = status,
        formVersion = formVersion,
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
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
