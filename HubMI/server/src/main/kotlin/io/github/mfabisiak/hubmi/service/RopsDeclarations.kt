package io.github.mfabisiak.hubmi.service

import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.DeclarationDto
import io.github.mfabisiak.hubmi.api.DeclarationsResponse

object RopsDeclarations {
    const val FORM_VERSION = 1

    val rodoDeclarations =
        listOf(
            DeclarationDto(
                id = "RODO_ROPS",
                category = "RODO",
                text =
                    "Oświadczam, że zapoznałem/am się z klauzulą informacyjną Regionalnego Ośrodka Polityki " +
                        "Społecznej w Krakowie dotyczącą przetwarzania danych osobowych.",
                required = true,
            ),
            DeclarationDto(
                id = "RODO_MINISTRY",
                category = "RODO",
                text =
                    "Oświadczam, że zapoznałem/am się z klauzulą informacyjną ministra właściwego ds. rozwoju " +
                        "regionalnego dotyczącą przetwarzania danych osobowych w ramach programu.",
                required = true,
            ),
        )

    val individualDeclarations =
        listOf(
            DeclarationDto(
                id = "INDIV_TRUTH",
                category = "Oświadczenia wnioskodawcy (osoba fizyczna)",
                text = "Oświadczam, że wszelkie informacje podane we wniosku są zgodne ze stanem faktycznym i prawnym.",
                required = true,
            ),
            DeclarationDto(
                id = "INDIV_NO_DOUBLE_FINANCING",
                category = "Oświadczenia wnioskodawcy (osoba fizyczna)",
                text =
                    "Oświadczam, że działania objęte wnioskiem nie są i nie będą współfinansowane z innych " +
                        "środków publicznych (zakaz podwójnego finansowania).",
                required = true,
            ),
            DeclarationDto(
                id = "INDIV_CAPACITY",
                category = "Oświadczenia wnioskodawcy (osoba fizyczna)",
                text =
                    "Oświadczam, że posiadam pełną zdolność do czynności prawnych i korzystam z pełni " +
                        "praw publicznych.",
                required = true,
            ),
            DeclarationDto(
                id = "INDIV_RULES_ACCEPTANCE",
                category = "Oświadczenia wnioskodawcy (osoba fizyczna)",
                text =
                    "Oświadczam, że zapoznałem/am się z Regulaminem naboru i realizacji mikroinnowacji " +
                        "i akceptuję jego postanowienia.",
                required = true,
            ),
        )

    val entityDeclarations =
        listOf(
            DeclarationDto(
                id = "ENTITY_TRUTH",
                category = "Oświadczenia reprezentanta podmiotu",
                text =
                    "Oświadczam, że reprezentowany podmiot działa zgodnie z prawem, a dane zawarte we wniosku " +
                        "są zgodne ze stanem faktycznym i prawnym.",
                required = true,
            ),
            DeclarationDto(
                id = "ENTITY_NO_DOUBLE_FINANCING",
                category = "Oświadczenia reprezentanta podmiotu",
                text =
                    "Oświadczam, że wydatki ujęte w budżecie innowacji nie są i nie będą finansowane z innych " +
                        "źródeł publicznych.",
                required = true,
            ),
            DeclarationDto(
                id = "ENTITY_NOT_EXCLUDED",
                category = "Oświadczenia reprezentanta podmiotu",
                text =
                    "Oświadczam, że reprezentowany podmiot nie podlega wykluczeniu z możliwości otrzymania " +
                        "dofinansowania.",
                required = true,
            ),
            DeclarationDto(
                id = "ENTITY_RULES_ACCEPTANCE",
                category = "Oświadczenia reprezentanta podmiotu",
                text =
                    "Oświadczam, że podmiot akceptuje w całości Regulamin naboru i realizacji mikroinnowacji.",
                required = true,
            ),
        )

    val groupDeclarations =
        listOf(
            DeclarationDto(
                id = "GROUP_TRUTH",
                category = "Oświadczenia grupy nieformalnej",
                text =
                    "Oświadczam w imieniu grupy nieformalnej, że informacje zawarte we wniosku są zgodne " +
                        "ze stanem faktycznym.",
                required = true,
            ),
            DeclarationDto(
                id = "GROUP_NO_DOUBLE_FINANCING",
                category = "Oświadczenia grupy nieformalnej",
                text =
                    "Oświadczamy, że planowane działania nie podlegają podwójnemu finansowaniu ze środków publicznych.",
                required = true,
            ),
            DeclarationDto(
                id = "GROUP_PARTNERSHIP",
                category = "Oświadczenia grupy nieformalnej",
                text =
                    "Oświadczam, że wszyscy członkowie grupy nieformalnej wyrazili zgodę na reprezentowanie " +
                        "ich w naborze.",
                required = true,
            ),
            DeclarationDto(
                id = "GROUP_RULES_ACCEPTANCE",
                category = "Oświadczenia grupy nieformalnej",
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

    fun getRequiredIds(applicantType: ApplicantType): Set<String> {
        val specific =
            when (applicantType) {
                ApplicantType.INDIVIDUAL -> individualDeclarations
                ApplicantType.ENTITY -> entityDeclarations
                ApplicantType.NON_FORMAL_GROUP -> groupDeclarations
            }
        return (specific + rodoDeclarations).filter { it.required }.map { it.id }.toSet()
    }
}
