package io.github.mfabisiak.hubmi.calls

import io.github.mfabisiak.hubmi.api.ActionPlanDto
import io.github.mfabisiak.hubmi.api.AddressDto
import io.github.mfabisiak.hubmi.api.ApplicantDto
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ContactPersonDto
import io.github.mfabisiak.hubmi.api.EntityApplicantDto
import io.github.mfabisiak.hubmi.api.EntityPartnerDto
import io.github.mfabisiak.hubmi.api.GroupRepresentativeDto
import io.github.mfabisiak.hubmi.api.IndividualApplicantDto
import io.github.mfabisiak.hubmi.api.IndividualPartnerDto
import io.github.mfabisiak.hubmi.api.NonFormalGroupApplicantDto
import io.github.mfabisiak.hubmi.api.PartnerDto
import io.github.mfabisiak.hubmi.api.PlanItemDto

fun AddressItem.toDto(): AddressDto =
    AddressDto(
        street = street,
        buildingNumber = buildingNumber,
        apartmentNumber = apartmentNumber,
        postalCode = postalCode,
        city = city,
    )

fun AddressDto.toItem(): AddressItem =
    AddressItem(
        street = street.trim(),
        buildingNumber = buildingNumber.trim(),
        apartmentNumber = apartmentNumber?.trim(),
        postalCode = postalCode.trim(),
        city = city.trim(),
    )

fun ContactPersonItem.toDto(): ContactPersonDto =
    ContactPersonDto(
        function = function,
        fullName = fullName,
        phone = phone,
        email = email,
    )

fun ContactPersonDto.toItem(): ContactPersonItem =
    ContactPersonItem(
        function = function.trim(),
        fullName = fullName.trim(),
        phone = phone.trim(),
        email = email.trim(),
    )

fun GroupRepresentativeItem.toDto(): GroupRepresentativeDto =
    GroupRepresentativeDto(
        firstName = firstName,
        lastName = lastName,
        phone = phone,
        email = email,
    )

fun GroupRepresentativeDto.toItem(): GroupRepresentativeItem =
    GroupRepresentativeItem(
        firstName = firstName.trim(),
        lastName = lastName.trim(),
        phone = phone.trim(),
        email = email.trim(),
    )

fun PartnerItem.toDto(): PartnerDto =
    when (this) {
        is IndividualPartnerItem -> {
            IndividualPartnerDto(
                firstName = firstName,
                lastName = lastName,
                address = address.toDto(),
                phone = phone,
                email = email,
            )
        }

        is EntityPartnerItem -> {
            EntityPartnerDto(
                name = name,
                krs = krs,
                regon = regon,
                nip = nip,
                address = address.toDto(),
                phone = phone,
                email = email,
            )
        }
    }

fun PartnerDto.toItem(): PartnerItem =
    when (this) {
        is IndividualPartnerDto -> {
            IndividualPartnerItem(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                address = address.toItem(),
                phone = phone.trim(),
                email = email.trim(),
            )
        }

        is EntityPartnerDto -> {
            EntityPartnerItem(
                name = name.trim(),
                krs = krs.trim(),
                regon = regon.trim(),
                nip = nip.trim(),
                address = address.toItem(),
                phone = phone.trim(),
                email = email.trim(),
            )
        }
    }

fun ApplicantItem.toDto(): ApplicantDto =
    when (this) {
        is IndividualApplicantItem -> {
            IndividualApplicantDto(
                firstName = firstName,
                lastName = lastName,
                address = address.toDto(),
                phone = phone,
                email = email,
            )
        }

        is EntityApplicantItem -> {
            EntityApplicantDto(
                name = name,
                krs = krs,
                regon = regon,
                nip = nip,
                address = address.toDto(),
                phone = phone,
                email = email,
                representative = representative.toDto(),
                contactPerson = contactPerson.toDto(),
            )
        }

        is NonFormalGroupApplicantItem -> {
            NonFormalGroupApplicantDto(
                partners = partners.map { it.toDto() },
                representative = representative.toDto(),
            )
        }
    }

fun ApplicantDto.toItem(): ApplicantItem =
    when (this) {
        is IndividualApplicantDto -> {
            IndividualApplicantItem(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                address = address.toItem(),
                phone = phone.trim(),
                email = email.trim(),
            )
        }

        is EntityApplicantDto -> {
            EntityApplicantItem(
                name = name.trim(),
                krs = krs.trim(),
                regon = regon.trim(),
                nip = nip.trim(),
                address = address.toItem(),
                phone = phone.trim(),
                email = email.trim(),
                representative = representative.toItem(),
                contactPerson = contactPerson.toItem(),
            )
        }

        is NonFormalGroupApplicantDto -> {
            NonFormalGroupApplicantItem(
                partners = partners.map { it.toItem() },
                representative = representative.toItem(),
            )
        }
    }

fun PlanItem.toDto(): PlanItemDto =
    PlanItemDto(
        action = action,
        term = term,
        costGrosze = costGrosze,
    )

fun PlanItemDto.toItem(): PlanItem =
    PlanItem(
        action = action.trim(),
        term = term.trim(),
        costGrosze = costGrosze,
    )

fun ActionPlanItem.toDto(): ActionPlanDto =
    ActionPlanDto(
        preparation = preparation.map { it.toDto() },
        testingPhase1 = testingPhase1.map { it.toDto() },
        testingPhase2 = testingPhase2.map { it.toDto() },
    )

fun ActionPlanDto.toItem(): ActionPlanItem =
    ActionPlanItem(
        preparation = preparation.map { it.toItem() },
        testingPhase1 = testingPhase1.map { it.toItem() },
        testingPhase2 = testingPhase2.map { it.toItem() },
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
        applicant = applicant?.toDto(),
        description = description,
        innovativeness = innovativeness,
        problemDiagnosis = problemDiagnosis,
        socialArea = socialArea,
        audienceDescription = audienceDescription,
        expectedChange = expectedChange,
        futureVision = futureVision,
        plan = plan?.toDto(),
        requestedGrantAmountGrosze = requestedGrantAmountGrosze,
        projectTeam = projectTeam,
        declarations = declarations,
        submittedAt = submittedAt?.toString(),
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
