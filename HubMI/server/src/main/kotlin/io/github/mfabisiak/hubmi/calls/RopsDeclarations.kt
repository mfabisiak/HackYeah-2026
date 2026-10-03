package io.github.mfabisiak.hubmi.calls

import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.DeclarationCategory
import io.github.mfabisiak.hubmi.api.DeclarationDto
import io.github.mfabisiak.hubmi.api.DeclarationId
import io.github.mfabisiak.hubmi.api.DeclarationsResponse

object RopsDeclarations {
    const val FORM_VERSION = 1

    val rodoDeclarations =
        listOf(
            DeclarationDto(
                id = DeclarationId.RODO_ROPS,
                category = DeclarationCategory.RODO,
                text =
                    "Oświadczam, że zapoznałem/am się z klauzulą informacyjną Regionalnego Ośrodka Polityki " +
                        "Społecznej w Krakowie dotyczącą przetwarzania danych osobowych.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.RODO_MINISTRY,
                category = DeclarationCategory.RODO,
                text =
                    "Oświadczam, że zapoznałem/am się z klauzulą informacyjną ministra właściwego ds. rozwoju " +
                        "regionalnego dotyczącą przetwarzania danych osobowych w ramach programu.",
                required = true,
            ),
        )

    val individualDeclarations =
        listOf(
            DeclarationDto(
                id = DeclarationId.INDIV_TRUTH,
                category = DeclarationCategory.INDIVIDUAL,
                text = "Oświadczam, że wszelkie informacje podane we wniosku są zgodne ze stanem faktycznym i prawnym.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.INDIV_NO_DOUBLE_FINANCING,
                category = DeclarationCategory.INDIVIDUAL,
                text =
                    "Oświadczam, że działania objęte wnioskiem nie są i nie będą współfinansowane z innych " +
                        "środków publicznych (zakaz podwójnego finansowania).",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.INDIV_CAPACITY,
                category = DeclarationCategory.INDIVIDUAL,
                text =
                    "Oświadczam, że posiadam pełną zdolność do czynności prawnych i korzystam z pełni " +
                        "praw publicznych.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.INDIV_RULES_ACCEPTANCE,
                category = DeclarationCategory.INDIVIDUAL,
                text =
                    "Oświadczam, że zapoznałem/am się z Regulaminem naboru i realizacji mikroinnowacji " +
                        "i akceptuję jego postanowienia.",
                required = true,
            ),
        )

    val entityDeclarations =
        listOf(
            DeclarationDto(
                id = DeclarationId.ENTITY_TRUTH,
                category = DeclarationCategory.ENTITY,
                text =
                    "Oświadczam, że reprezentowany podmiot działa zgodnie z prawem, a dane zawarte we wniosku " +
                        "są zgodne ze stanem faktycznym i prawnym.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.ENTITY_NO_DOUBLE_FINANCING,
                category = DeclarationCategory.ENTITY,
                text =
                    "Oświadczam, że wydatki ujęte w budżecie innowacji nie są i nie będą finansowane z innych " +
                        "źródeł publicznych.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.ENTITY_NOT_EXCLUDED,
                category = DeclarationCategory.ENTITY,
                text =
                    "Oświadczam, że reprezentowany podmiot nie podlega wykluczeniu z możliwości otrzymania " +
                        "dofinansowania.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.ENTITY_RULES_ACCEPTANCE,
                category = DeclarationCategory.ENTITY,
                text =
                    "Oświadczam, że podmiot akceptuje w całości Regulamin naboru i realizacji mikroinnowacji.",
                required = true,
            ),
        )

    val groupDeclarations =
        listOf(
            DeclarationDto(
                id = DeclarationId.GROUP_TRUTH,
                category = DeclarationCategory.NON_FORMAL_GROUP,
                text =
                    "Oświadczam w imieniu grupy nieformalnej, że informacje zawarte we wniosku są zgodne " +
                        "ze stanem faktycznym.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.GROUP_NO_DOUBLE_FINANCING,
                category = DeclarationCategory.NON_FORMAL_GROUP,
                text =
                    "Oświadczamy, że planowane działania nie podlegają podwójnemu finansowaniu ze środków publicznych.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.GROUP_PARTNERSHIP,
                category = DeclarationCategory.NON_FORMAL_GROUP,
                text =
                    "Oświadczam, że wszyscy członkowie grupy nieformalnej wyrazili zgodę na reprezentowanie " +
                        "ich w naborze.",
                required = true,
            ),
            DeclarationDto(
                id = DeclarationId.GROUP_RULES_ACCEPTANCE,
                category = DeclarationCategory.NON_FORMAL_GROUP,
                text =
                    "Oświadczamy, że zapoznaliśmy się z Regulaminem naboru i akceptujemy jego warunki.",
                required = true,
            ),
        )

    fun getDeclarations(applicantType: ApplicantType?): DeclarationsResponse {
        val specific =
            when (applicantType) {
                ApplicantType.INDIVIDUAL -> individualDeclarations
                ApplicantType.ENTITY -> entityDeclarations
                ApplicantType.NON_FORMAL_GROUP -> groupDeclarations
                null -> individualDeclarations + entityDeclarations + groupDeclarations
            }
        return DeclarationsResponse(
            formVersion = FORM_VERSION,
            declarations = specific + rodoDeclarations,
        )
    }

    fun getRequiredIds(applicantType: ApplicantType): Set<DeclarationId> {
        val specific =
            when (applicantType) {
                ApplicantType.INDIVIDUAL -> individualDeclarations
                ApplicantType.ENTITY -> entityDeclarations
                ApplicantType.NON_FORMAL_GROUP -> groupDeclarations
            }
        return (specific + rodoDeclarations).filter { it.required }.map { it.id }.toSet()
    }
}
