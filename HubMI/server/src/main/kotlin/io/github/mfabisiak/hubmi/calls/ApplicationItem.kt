package io.github.mfabisiak.hubmi.calls

import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.ApplicationStatus
import io.github.mfabisiak.hubmi.api.DeclarationId
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.common.mongo.JavaInstantAsBsonDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.Instant

@Serializable
data class AddressItem(
    val street: String,
    val buildingNumber: String,
    val apartmentNumber: String? = null,
    val postalCode: String,
    val city: String,
)

@Serializable
data class ContactPersonItem(
    val function: String,
    val fullName: String,
    val phone: String,
    val email: String,
)

@Serializable
data class GroupRepresentativeItem(
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
)

@Serializable
sealed interface ApplicantItem {
    val type: ApplicantType
}

@Serializable
@SerialName("INDIVIDUAL")
data class IndividualApplicantItem(
    val firstName: String,
    val lastName: String,
    val address: AddressItem,
    val phone: String,
    val email: String,
) : ApplicantItem {
    override val type: ApplicantType get() = ApplicantType.INDIVIDUAL
}

@Serializable
@SerialName("ENTITY")
data class EntityApplicantItem(
    val name: String,
    val krs: String,
    val regon: String,
    val nip: String,
    val address: AddressItem,
    val phone: String,
    val email: String,
    val representative: ContactPersonItem,
    val contactPerson: ContactPersonItem,
) : ApplicantItem {
    override val type: ApplicantType get() = ApplicantType.ENTITY
}

@Serializable
sealed interface PartnerItem

@Serializable
@SerialName("PARTNER_INDIVIDUAL")
data class IndividualPartnerItem(
    val firstName: String,
    val lastName: String,
    val address: AddressItem,
    val phone: String,
    val email: String,
) : PartnerItem

@Serializable
@SerialName("PARTNER_ENTITY")
data class EntityPartnerItem(
    val name: String,
    val krs: String,
    val regon: String,
    val nip: String,
    val address: AddressItem,
    val phone: String,
    val email: String,
) : PartnerItem

@Serializable
@SerialName("NON_FORMAL_GROUP")
data class NonFormalGroupApplicantItem(
    val partners: List<PartnerItem>,
    val representative: GroupRepresentativeItem,
) : ApplicantItem {
    override val type: ApplicantType get() = ApplicantType.NON_FORMAL_GROUP
}

@Serializable
data class PlanItem(
    val action: String,
    val term: String,
    val costGrosze: Int,
)

@Serializable
data class ActionPlanItem(
    val preparation: List<PlanItem> = emptyList(),
    val testingPhase1: List<PlanItem> = emptyList(),
    val testingPhase2: List<PlanItem> = emptyList(),
)

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
    val applicant: ApplicantItem? = null,
    val description: String? = null,
    val innovativeness: String? = null,
    val problemDiagnosis: String? = null,
    val socialArea: SocialArea? = null,
    val audienceDescription: String? = null,
    val expectedChange: String? = null,
    val futureVision: String? = null,
    val plan: ActionPlanItem? = null,
    val requestedGrantAmountGrosze: Int? = null,
    val projectTeam: String? = null,
    val declarations: List<DeclarationId> = emptyList(),
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val submittedAt: Instant? = null,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val createdAt: Instant,
    @Serializable(with = JavaInstantAsBsonDateTime::class)
    val updatedAt: Instant,
)
