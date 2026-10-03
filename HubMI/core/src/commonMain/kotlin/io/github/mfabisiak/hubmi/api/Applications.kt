package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Module 3: Grant Applications according to official ROPS template (Załącznik nr 3)

@Serializable
enum class ApplicationStatus {
    DRAFT,
    SUBMITTED,
}

@Serializable
enum class ApplicantType {
    INDIVIDUAL,
    ENTITY,
    NON_FORMAL_GROUP,
}

@Serializable
@Resource("applications")
class Applications(
    val parent: Api = Api(),
) {
    /** `GET` (authenticated): applications submitted or drafted by the caller. */
    @Serializable
    @Resource("mine")
    class Mine(
        val parent: Applications = Applications(),
        val page: Int = 0,
        val size: Int = 20,
    )

    /** `GET` (authenticated, author or admin), `PUT` (authenticated author, draft only). */
    @Serializable
    @Resource("{id}")
    class ById(
        val parent: Applications = Applications(),
        val id: String,
    ) {
        /** `POST` (authenticated, author): submit the draft application. */
        @Serializable
        @Resource("submit")
        class Submit(
            val parent: ById,
        )
    }
}

@Serializable
data class AddressDto(
    val street: String,
    val buildingNumber: String,
    val apartmentNumber: String? = null,
    val postalCode: String,
    val city: String,
)

@Serializable
data class ContactPersonDto(
    val function: String,
    val fullName: String,
    val phone: String,
    val email: String,
)

@Serializable
sealed interface ApplicantDto {
    val type: ApplicantType
}

@Serializable
@SerialName("individual")
data class IndividualApplicantDto(
    val firstName: String,
    val lastName: String,
    val address: AddressDto,
    val phone: String,
    val email: String,
) : ApplicantDto {
    override val type: ApplicantType get() = ApplicantType.INDIVIDUAL
}

@Serializable
@SerialName("entity")
data class EntityApplicantDto(
    val name: String,
    val krs: String,
    val regon: String,
    val nip: String,
    val address: AddressDto,
    val phone: String,
    val email: String,
    val representative: ContactPersonDto,
    val contactPerson: ContactPersonDto,
) : ApplicantDto {
    override val type: ApplicantType get() = ApplicantType.ENTITY
}

@Serializable
sealed interface PartnerDto

@Serializable
@SerialName("partner_individual")
data class IndividualPartnerDto(
    val firstName: String,
    val lastName: String,
    val address: AddressDto,
    val phone: String,
    val email: String,
) : PartnerDto

@Serializable
@SerialName("partner_entity")
data class EntityPartnerDto(
    val name: String,
    val krs: String,
    val regon: String,
    val nip: String,
    val address: AddressDto,
    val phone: String,
    val email: String,
) : PartnerDto

@Serializable
@SerialName("non_formal_group")
data class NonFormalGroupApplicantDto(
    val partners: List<PartnerDto>,
    val representative: ContactPersonDto,
) : ApplicantDto {
    override val type: ApplicantType get() = ApplicantType.NON_FORMAL_GROUP
}

@Serializable
data class PlanItemDto(
    val action: String,
    /** Month in YYYY-MM format, e.g. "2026-04". */
    val term: String,
    /** Cost in grosze (1 PLN = 100 groszy). */
    val costGrosze: Int,
)

@Serializable
data class ActionPlanDto(
    /** Preparation period (max 3 months duration). */
    val preparation: List<PlanItemDto> = emptyList(),
    /** Testing Phase 1. */
    val testingPhase1: List<PlanItemDto> = emptyList(),
    /** Testing Phase 2. Total testing duration (Phase 1 + Phase 2) max 9 months. */
    val testingPhase2: List<PlanItemDto> = emptyList(),
)

@Serializable
data class CreateApplicationDraftRequest(
    val ideaId: String? = null,
)

typealias CreateApplicationRequest = CreateApplicationDraftRequest

@Serializable
data class SaveApplicationDraftRequest(
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
)

@Serializable
data class ApplicationDto(
    val id: String,
    val callId: String,
    val applicantId: String,
    val ideaId: String? = null,
    val status: ApplicationStatus,
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
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class DeclarationDto(
    val id: String,
    val category: String,
    val text: String,
    val required: Boolean = true,
)

@Serializable
data class DeclarationsResponse(
    val formVersion: Int,
    val declarations: List<DeclarationDto>,
)
