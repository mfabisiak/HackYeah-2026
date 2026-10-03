package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.toNonEmptyListOrNull
import io.github.mfabisiak.hubmi.api.DeclarationId
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.asNel

object RopsApplicationValidator {
    const val MAX_NARRATIVE_LENGTH = 5000
    const val MAX_TITLE_LENGTH = 200
    const val MAX_PREPARATION_MONTHS = 3
    const val MAX_TESTING_MONTHS = 9
    const val MAX_ITEM_COST_GROSZE = 10_000_000L // 100 000 PLN max per item
    const val MAX_GRANT_AMOUNT_GROSZE = 10_000_000 // 100 000 PLN max total grant

    private val TERM_REGEX = Regex("""^\d{4}-(0[1-9]|1[0-2])$""")

    fun validateForSubmission(app: ApplicationItem): Either<DomainError.Validation, Unit> {
        val generalPart =
            Either.zipOrAccumulate(
                validateTitle(app.title).asNel(),
                validateApplicant(app.applicant),
                validateNarrative("description", "Opis innowacji", app.description).asNel(),
                validateNarrative("innovativeness", "Innowacyjność rozwiązania", app.innovativeness).asNel(),
                validateNarrative("problemDiagnosis", "Diagnoza problemu", app.problemDiagnosis).asNel(),
                validateSocialArea(app.socialArea).asNel(),
            ) { _, _, _, _, _, _ -> Unit }

        val detailsPart =
            Either.zipOrAccumulate(
                validateNarrative("audienceDescription", "Opis odbiorców innowacji", app.audienceDescription).asNel(),
                validateNarrative("expectedChange", "Zmiana, jaką wprowadza innowacja", app.expectedChange).asNel(),
                validateNarrative("futureVision", "Wizja przyszłości innowacji", app.futureVision).asNel(),
                validatePlanAndBudget(app.plan, app.requestedGrantAmountGrosze),
                validateNarrative("projectTeam", "Zespół projektowy", app.projectTeam).asNel(),
                validateDeclarations(app.applicant, app.declarations),
            ) { _, _, _, _, _, _ -> Unit }

        return Either
            .zipOrAccumulate(generalPart, detailsPart) { _, _ -> Unit }
            .mapLeft { errors: Nel<FieldError> ->
                DomainError.Validation(
                    message = "Formularz wniosku zawiera błędy walidacji",
                    details = errors.toList(),
                )
            }
    }

    private fun validateTitle(title: String?): Either<FieldError, String> =
        either {
            val trimmed = title?.trim()
            ensure(!trimmed.isNullOrBlank()) {
                FieldError("title", FieldErrorCode.Blank, "Tytuł innowacji jest wymagany")
            }
            ensure(trimmed.length <= MAX_TITLE_LENGTH) {
                FieldError(
                    "title",
                    FieldErrorCode.InvalidFormat,
                    "Tytuł innowacji nie może przekraczać $MAX_TITLE_LENGTH znaków",
                )
            }
            trimmed
        }

    private fun validateSocialArea(socialArea: SocialArea?): Either<FieldError, SocialArea> =
        either {
            ensure(socialArea != null) {
                FieldError(
                    "socialArea",
                    FieldErrorCode.Blank,
                    "Wskazanie obszaru społecznego (SocialArea) z Mapy Wyzwań jest wymagane",
                )
            }
            socialArea
        }

    private fun validateNarrative(
        field: String,
        label: String,
        value: String?,
    ): Either<FieldError, String> =
        either {
            val trimmed = value?.trim()
            ensure(!trimmed.isNullOrBlank()) {
                FieldError(field, FieldErrorCode.Blank, "Pole '$label' jest wymagane")
            }
            ensure(trimmed.length <= MAX_NARRATIVE_LENGTH) {
                FieldError(
                    field,
                    FieldErrorCode.InvalidFormat,
                    "Pole '$label' nie może przekraczać $MAX_NARRATIVE_LENGTH znaków",
                )
            }
            trimmed
        }

    private fun validateApplicant(applicant: ApplicantItem?): EitherNel<FieldError, ApplicantItem> =
        if (applicant == null) {
            Either.Left(
                NonEmptyList(
                    FieldError("applicant", FieldErrorCode.Blank, "Dane wnioskodawcy są wymagane"),
                    emptyList(),
                ),
            )
        } else {
            when (applicant) {
                is IndividualApplicantItem -> validateIndividual(applicant)
                is EntityApplicantItem -> validateEntity(applicant, "applicant")
                is NonFormalGroupApplicantItem -> validateNonFormalGroup(applicant)
            }
        }

    private fun validateIndividual(applicant: IndividualApplicantItem): EitherNel<FieldError, ApplicantItem> =
        Either.zipOrAccumulate(
            validateNonBlank("applicant.firstName", "Imię wnioskodawcy", applicant.firstName).asNel(),
            validateNonBlank("applicant.lastName", "Nazwisko wnioskodawcy", applicant.lastName).asNel(),
            validateAddress("applicant.address", applicant.address),
            Phone.parse("applicant.phone", applicant.phone).asNel(),
            Email.parse("applicant.email", applicant.email).asNel(),
        ) { _, _, _, _, _ -> applicant }

    private fun validateEntity(
        applicant: EntityApplicantItem,
        prefix: String,
    ): EitherNel<FieldError, ApplicantItem> =
        Either.zipOrAccumulate(
            validateNonBlank("$prefix.name", "Nazwa podmiotu", applicant.name).asNel(),
            Krs.parse("$prefix.krs", applicant.krs).asNel(),
            Regon.parse("$prefix.regon", applicant.regon).asNel(),
            Nip.parse("$prefix.nip", applicant.nip).asNel(),
            validateAddress("$prefix.address", applicant.address),
            Phone.parse("$prefix.phone", applicant.phone).asNel(),
            Email.parse("$prefix.email", applicant.email).asNel(),
            validateContactPerson("$prefix.representative", "osoby reprezentującej", applicant.representative),
            validateContactPerson("$prefix.contactPerson", "osoby do kontaktu", applicant.contactPerson),
        ) { _, _, _, _, _, _, _, _, _ -> applicant }

    private fun validateNonFormalGroup(group: NonFormalGroupApplicantItem): EitherNel<FieldError, ApplicantItem> =
        Either.zipOrAccumulate(
            validatePartners(group.partners),
            validateGroupRepresentative("applicant.representative", group.representative),
        ) { _, _ -> group }

    private fun validatePartners(partners: List<PartnerItem>): EitherNel<FieldError, List<PartnerItem>> =
        if (partners.size !in 1..5) {
            Either.Left(
                NonEmptyList(
                    FieldError(
                        "applicant.partners",
                        FieldErrorCode.Range(1, 5),
                        "Grupa nieformalna musi liczyć od 1 do 5 partnerów",
                    ),
                    emptyList(),
                ),
            )
        } else {
            partners
                .mapIndexed { idx, partner ->
                    when (partner) {
                        is IndividualPartnerItem -> {
                            Either.zipOrAccumulate(
                                validateNonBlank(
                                    "applicant.partners[$idx].firstName",
                                    "Imię partnera",
                                    partner.firstName,
                                ).asNel(),
                                validateNonBlank(
                                    "applicant.partners[$idx].lastName",
                                    "Nazwisko partnera",
                                    partner.lastName,
                                ).asNel(),
                                validateAddress("applicant.partners[$idx].address", partner.address),
                                Phone.parse("applicant.partners[$idx].phone", partner.phone).asNel(),
                                Email.parse("applicant.partners[$idx].email", partner.email).asNel(),
                            ) { _, _, _, _, _ -> partner }
                        }

                        is EntityPartnerItem -> {
                            Either.zipOrAccumulate(
                                validateNonBlank(
                                    "applicant.partners[$idx].name",
                                    "Nazwa podmiotu partnera",
                                    partner.name,
                                ).asNel(),
                                Krs.parse("applicant.partners[$idx].krs", partner.krs).asNel(),
                                Regon.parse("applicant.partners[$idx].regon", partner.regon).asNel(),
                                Nip.parse("applicant.partners[$idx].nip", partner.nip).asNel(),
                                validateAddress("applicant.partners[$idx].address", partner.address),
                                Phone.parse("applicant.partners[$idx].phone", partner.phone).asNel(),
                                Email.parse("applicant.partners[$idx].email", partner.email).asNel(),
                            ) { _, _, _, _, _, _, _ -> partner }
                        }
                    }
                }.let { results ->
                    val errors = results.filterIsInstance<Either.Left<Nel<FieldError>>>().flatMap { it.value.toList() }
                    if (errors.isNotEmpty()) {
                        Either.Left(NonEmptyList(errors.first(), errors.drop(1)))
                    } else {
                        Either.Right(partners)
                    }
                }
        }

    private fun validateGroupRepresentative(
        prefix: String,
        rep: GroupRepresentativeItem,
    ): EitherNel<FieldError, GroupRepresentativeItem> =
        Either.zipOrAccumulate(
            validateNonBlank("$prefix.firstName", "Imię reprezentanta grupy", rep.firstName).asNel(),
            validateNonBlank("$prefix.lastName", "Nazwisko reprezentanta grupy", rep.lastName).asNel(),
            Phone.parse("$prefix.phone", rep.phone).asNel(),
            Email.parse("$prefix.email", rep.email).asNel(),
        ) { _, _, _, _ -> rep }

    private fun validateContactPerson(
        prefix: String,
        label: String,
        person: ContactPersonItem,
    ): EitherNel<FieldError, ContactPersonItem> =
        Either.zipOrAccumulate(
            validateNonBlank("$prefix.function", "Funkcja $label", person.function).asNel(),
            validateNonBlank("$prefix.fullName", "Imię i nazwisko $label", person.fullName).asNel(),
            Phone.parse("$prefix.phone", person.phone).asNel(),
            Email.parse("$prefix.email", person.email).asNel(),
        ) { _, _, _, _ -> person }

    private fun validateAddress(
        prefix: String,
        address: AddressItem,
    ): EitherNel<FieldError, AddressItem> =
        Either.zipOrAccumulate(
            validateNonBlank("$prefix.street", "Ulica", address.street).asNel(),
            validateNonBlank("$prefix.buildingNumber", "Numer budynku", address.buildingNumber).asNel(),
            PostalCode.parse("$prefix.postalCode", address.postalCode).asNel(),
            validateNonBlank("$prefix.city", "Miejscowość", address.city).asNel(),
        ) { _, _, _, _ -> address }

    private fun validateNonBlank(
        field: String,
        label: String,
        value: String?,
    ): Either<FieldError, String> =
        either {
            val trimmed = value?.trim()
            ensure(!trimmed.isNullOrBlank()) {
                FieldError(field, FieldErrorCode.Blank, "Pole '$label' jest wymagane")
            }
            trimmed
        }

    private fun validatePlanAndBudget(
        plan: ActionPlanItem?,
        requestedGrantAmountGrosze: Int?,
    ): EitherNel<FieldError, Unit> =
        when (plan) {
            null -> {
                nonEmptyListOf(
                    FieldError("plan", FieldErrorCode.Blank, "Harmonogram i budżet projektu są wymagane"),
                ).left()
            }

            else -> {
                (
                    planCompletenessErrors(plan) +
                        planTermsErrors(plan) +
                        planDurationErrors(plan) +
                        sequenceErrors(plan) +
                        planCostErrors(plan) +
                        grantAmountErrors(plan, requestedGrantAmountGrosze)
                ).asValidation()
            }
        }

    private fun planCompletenessErrors(plan: ActionPlanItem): List<FieldError> =
        listOfNotNull(
            FieldError(
                "plan.preparation",
                FieldErrorCode.Blank,
                "Wymagane jest zdefiniowanie co najmniej jednego zadania w okresie przygotowawczym",
            ).takeIf { plan.preparation.isEmpty() },
            FieldError(
                "plan.testingPhase1",
                FieldErrorCode.Blank,
                "Wymagane jest zdefiniowanie co najmniej jednego zadania w Fazie I testowania",
            ).takeIf { plan.testingPhase1.isEmpty() },
        )

    private fun planTermsErrors(plan: ActionPlanItem): List<FieldError> =
        termsFormatErrors(plan.preparation, "plan.preparation") +
            termsFormatErrors(plan.testingPhase1, "plan.testingPhase1") +
            termsFormatErrors(plan.testingPhase2, "plan.testingPhase2")

    private fun planDurationErrors(plan: ActionPlanItem): List<FieldError> {
        val prepDuration = calculateDuration(plan.preparation)
        val testDuration = calculateDuration(plan.testingPhase1 + plan.testingPhase2)
        return listOfNotNull(
            FieldError(
                "plan.preparation",
                FieldErrorCode.Range(1, MAX_PREPARATION_MONTHS),
                "Okres przygotowawczy nie może przekraczać $MAX_PREPARATION_MONTHS miesięcy (obecnie: $prepDuration)",
            ).takeIf { prepDuration > MAX_PREPARATION_MONTHS },
            FieldError(
                "plan.testingPhase1",
                FieldErrorCode.Range(1, MAX_TESTING_MONTHS),
                "Łączny okres testowania (Faza I + Faza II) nie może przekraczać " +
                    "$MAX_TESTING_MONTHS miesięcy (obecnie: $testDuration)",
            ).takeIf { testDuration > MAX_TESTING_MONTHS },
        )
    }

    private data class IndexedTerm(
        val index: Int,
        val term: String,
    )

    private fun wellFormedTerms(items: List<PlanItem>): List<IndexedTerm> =
        items.mapIndexedNotNull { index, item ->
            item.term
                .trim()
                .takeIf(TERM_REGEX::matches)
                ?.let { IndexedTerm(index, it) }
        }

    /** The preparation precedes testing and Phase I precedes Phase II (terms are `YYYY-MM`, so they sort as text). */
    private fun sequenceErrors(plan: ActionPlanItem): List<FieldError> {
        val preparation = wellFormedTerms(plan.preparation)
        val phase1 = wellFormedTerms(plan.testingPhase1)
        val phase2 = wellFormedTerms(plan.testingPhase2)
        val firstTestingTerm = (phase1 + phase2).minOfOrNull(IndexedTerm::term)
        val firstPhase2Term = phase2.minOfOrNull(IndexedTerm::term)

        val preparationErrors =
            firstTestingTerm
                ?.let { first ->
                    preparation
                        .filter { it.term >= first }
                        .map {
                            FieldError(
                                field = "plan.preparation[${it.index}].term",
                                code = FieldErrorCode.InvalidFormat,
                                message = "Okres przygotowawczy (${it.term}) musi poprzedzać okres testowania ($first)",
                            )
                        }
                }.orEmpty()
        val phase1Errors =
            firstPhase2Term
                ?.let { first ->
                    phase1
                        .filter { it.term > first }
                        .map {
                            FieldError(
                                field = "plan.testingPhase1[${it.index}].term",
                                code = FieldErrorCode.InvalidFormat,
                                message = "Faza I testowania (${it.term}) musi poprzedzać Fazę II testowania ($first)",
                            )
                        }
                }.orEmpty()
        return preparationErrors + phase1Errors
    }

    private fun planCostErrors(plan: ActionPlanItem): List<FieldError> =
        costErrors(plan.preparation, "plan.preparation") +
            costErrors(plan.testingPhase1, "plan.testingPhase1") +
            costErrors(plan.testingPhase2, "plan.testingPhase2")

    private fun costErrors(
        items: List<PlanItem>,
        prefix: String,
    ): List<FieldError> =
        items.mapIndexedNotNull { index, item ->
            when {
                item.costGrosze <= 0 -> {
                    FieldError(
                        "$prefix[$index].costGrosze",
                        FieldErrorCode.InvalidFormat,
                        "Koszt zadania musi być większy od zera",
                    )
                }

                item.costGrosze.toLong() > MAX_ITEM_COST_GROSZE -> {
                    FieldError(
                        "$prefix[$index].costGrosze",
                        FieldErrorCode.InvalidFormat,
                        "Koszt pojedynczego zadania nie może przekraczać ${MAX_ITEM_COST_GROSZE / 100} PLN",
                    )
                }

                else -> {
                    null
                }
            }
        }

    private fun grantAmountErrors(
        plan: ActionPlanItem,
        requestedGrantAmountGrosze: Int?,
    ): List<FieldError> {
        val totalPlanCost =
            (plan.preparation + plan.testingPhase1 + plan.testingPhase2).sumOf {
                it.costGrosze.toLong()
            }
        val error =
            when {
                requestedGrantAmountGrosze == null || requestedGrantAmountGrosze <= 0 -> {
                    FieldError(
                        "requestedGrantAmountGrosze",
                        FieldErrorCode.Blank,
                        "Wnioskowana kwota grantu jest wymagana i musi być dodatnia",
                    )
                }

                requestedGrantAmountGrosze > MAX_GRANT_AMOUNT_GROSZE -> {
                    FieldError(
                        "requestedGrantAmountGrosze",
                        FieldErrorCode.InvalidFormat,
                        "Wnioskowana kwota grantu nie może przekraczać ${MAX_GRANT_AMOUNT_GROSZE / 100} PLN",
                    )
                }

                totalPlanCost != requestedGrantAmountGrosze.toLong() -> {
                    FieldError(
                        "requestedGrantAmountGrosze",
                        FieldErrorCode.InvalidFormat,
                        "Wnioskowana kwota grantu (${requestedGrantAmountGrosze / 100} PLN) musi być równa " +
                            "sumie kosztów z harmonogramu (${totalPlanCost / 100} PLN)",
                    )
                }

                else -> {
                    null
                }
            }
        return listOfNotNull(error)
    }

    private fun termsFormatErrors(
        items: List<PlanItem>,
        prefix: String,
    ): List<FieldError> =
        items.flatMapIndexed { index, item ->
            listOfNotNull(
                FieldError(
                    field = "$prefix[$index].action",
                    code = FieldErrorCode.Blank,
                    message = "Opis zadania nie może być pusty",
                ).takeIf { item.action.isBlank() },
                FieldError(
                    "$prefix[$index].term",
                    FieldErrorCode.InvalidFormat,
                    "Termin zadania musi mieć format RRRR-MM (np. 2026-04)",
                ).takeIf { !TERM_REGEX.matches(item.term.trim()) },
            )
        }

    private fun List<FieldError>.asValidation(): EitherNel<FieldError, Unit> =
        toNonEmptyListOrNull()?.let { Either.Left(it) } ?: Either.Right(Unit)

    private fun calculateDuration(items: List<PlanItem>): Int {
        val validTerms = items.map { it.term.trim() }.filter { TERM_REGEX.matches(it) }
        if (validTerms.isEmpty()) return 0
        val monthIndices =
            validTerms.map { term ->
                val parts = term.split("-")
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                year * 12 + month
            }
        return monthIndices.max() - monthIndices.min() + 1
    }

    private fun validateDeclarations(
        applicant: ApplicantItem?,
        declarations: List<DeclarationId>,
    ): EitherNel<FieldError, Unit> =
        when (applicant) {
            null -> {
                Either.Right(Unit)
            }

            else -> {
                val missing = RopsDeclarations.getRequiredIds(applicant.type) - declarations.toSet()
                listOfNotNull(
                    FieldError(
                        "declarations",
                        FieldErrorCode.InvalidFormat,
                        "Lista oświadczeń zawiera duplikaty",
                    ).takeIf { declarations.size != declarations.distinct().size },
                    FieldError(
                        field = "declarations",
                        code = FieldErrorCode.Required,
                        message =
                            "Wymagane jest zaakceptowanie wszystkich obowiązkowych oświadczeń. " +
                                "Brakujące: ${missing.joinToString(", ")}",
                    ).takeIf { missing.isNotEmpty() },
                ).asValidation()
            }
        }
}
