package io.github.mfabisiak.hubmi.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.ActionPlanDto
import io.github.mfabisiak.hubmi.api.AddressDto
import io.github.mfabisiak.hubmi.api.ApplicantDto
import io.github.mfabisiak.hubmi.api.ApplicantType
import io.github.mfabisiak.hubmi.api.ContactPersonDto
import io.github.mfabisiak.hubmi.api.EntityApplicantDto
import io.github.mfabisiak.hubmi.api.EntityPartnerDto
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.IndividualApplicantDto
import io.github.mfabisiak.hubmi.api.IndividualPartnerDto
import io.github.mfabisiak.hubmi.api.NonFormalGroupApplicantDto
import io.github.mfabisiak.hubmi.api.PartnerDto
import io.github.mfabisiak.hubmi.api.PlanItemDto
import io.github.mfabisiak.hubmi.models.ApplicationItem

object RopsApplicationValidator {
    const val MAX_NARRATIVE_LENGTH = 5000
    const val MAX_TITLE_LENGTH = 200
    const val MAX_PREPARATION_MONTHS = 3
    const val MAX_TESTING_MONTHS = 9

    private val POSTAL_CODE_REGEX = Regex("""^\d{2}-\d{3}$""")
    private val EMAIL_REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

    fun validateForSubmission(app: ApplicationItem): Either<DomainError.Validation, Unit> =
        either {
            val errors =
                buildList {
                    // Pkt 1: Tytuł
                    validateTitle(app.title, this)

                    // Pkt 2: Wnioskodawca
                    validateApplicant(app.applicant, this)

                    // Pkt 3-8: Pola narracyjne
                    validateNarrative("description", "Opis innowacji", app.description, this)
                    validateNarrative("innovativeness", "Innowacyjność rozwiązania", app.innovativeness, this)
                    validateNarrative("problemDiagnosis", "Diagnoza problemu", app.problemDiagnosis, this)
                    if (app.socialArea == null) {
                        add(
                            FieldError(
                                "socialArea",
                                FieldErrorCode.Blank,
                                "Wskazanie obszaru społecznego (SocialArea) z Mapy Wyzwań jest wymagane",
                            ),
                        )
                    }
                    validateNarrative("audienceDescription", "Opis odbiorców innowacji", app.audienceDescription, this)
                    validateNarrative("expectedChange", "Zmiana, jaką wprowadza innowacja", app.expectedChange, this)
                    validateNarrative("futureVision", "Wizja przyszłości innowacji", app.futureVision, this)

                    // Pkt 9 & 10: Harmonogram, budżet i wnioskowana kwota
                    validatePlanAndBudget(app.plan, app.requestedGrantAmountGrosze, this)

                    // Pkt 11: Zespół projektowy
                    validateNarrative("projectTeam", "Zespół projektowy", app.projectTeam, this)

                    // Pkt 12: Oświadczenia
                    validateDeclarations(app.applicant?.type, app.declarations, this)
                }

            ensure(errors.isEmpty()) {
                DomainError.Validation(
                    message = "Formularz wniosku zawiera błędy walidacji",
                    details = errors,
                )
            }
        }

    private fun validateTitle(
        title: String?,
        errors: MutableList<FieldError>,
    ) {
        if (title.isNullOrBlank()) {
            errors.add(FieldError("title", FieldErrorCode.Blank, "Tytuł innowacji jest wymagany"))
        } else if (title.length > MAX_TITLE_LENGTH) {
            errors.add(
                FieldError(
                    "title",
                    FieldErrorCode.InvalidFormat,
                    "Tytuł innowacji nie może przekraczać $MAX_TITLE_LENGTH znaków",
                ),
            )
        }
    }

    private fun validateNarrative(
        field: String,
        label: String,
        value: String?,
        errors: MutableList<FieldError>,
    ) {
        if (value.isNullOrBlank()) {
            errors.add(FieldError(field, FieldErrorCode.Blank, "Pole '$label' jest wymagane"))
        } else if (value.length > MAX_NARRATIVE_LENGTH) {
            errors.add(
                FieldError(
                    field,
                    FieldErrorCode.InvalidFormat,
                    "Pole '$label' nie może przekraczać $MAX_NARRATIVE_LENGTH znaków",
                ),
            )
        }
    }

    private fun validateApplicant(
        applicant: ApplicantDto?,
        errors: MutableList<FieldError>,
    ) {
        if (applicant == null) {
            errors.add(FieldError("applicant", FieldErrorCode.Blank, "Dane wnioskodawcy są wymagane"))
            return
        }

        when (applicant) {
            is IndividualApplicantDto -> validateIndividual(applicant, "applicant", errors)
            is EntityApplicantDto -> validateEntity(applicant, "applicant", errors)
            is NonFormalGroupApplicantDto -> validateNonFormalGroup(applicant, "applicant", errors)
        }
    }

    private fun validateIndividual(
        indiv: IndividualApplicantDto,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        if (indiv.firstName.isBlank()) {
            errors.add(FieldError("$path.firstName", FieldErrorCode.Blank, "Imię jest wymagane"))
        }
        if (indiv.lastName.isBlank()) {
            errors.add(FieldError("$path.lastName", FieldErrorCode.Blank, "Nazwisko jest wymagane"))
        }
        validateAddress(indiv.address, "$path.address", errors)
        validatePhone(indiv.phone, "$path.phone", errors)
        validateEmail(indiv.email, "$path.email", errors)
    }

    private fun validateEntity(
        entity: EntityApplicantDto,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        if (entity.name.isBlank()) {
            errors.add(FieldError("$path.name", FieldErrorCode.Blank, "Nazwa podmiotu jest wymagana"))
        }
        if (!isValidKrs(entity.krs)) {
            errors.add(FieldError("$path.krs", FieldErrorCode.InvalidFormat, "Numer KRS musi składać się z 10 cyfr"))
        }
        if (!isValidRegon(entity.regon)) {
            errors.add(
                FieldError(
                    "$path.regon",
                    FieldErrorCode.InvalidFormat,
                    "Nieprawidłowy numer REGON (wymagane 9 lub 14 cyfr z poprawną sumą kontrolną)",
                ),
            )
        }
        if (!isValidNip(entity.nip)) {
            errors.add(
                FieldError(
                    "$path.nip",
                    FieldErrorCode.InvalidFormat,
                    "Nieprawidłowy numer NIP (wymagane 10 cyfr z poprawną sumą kontrolną)",
                ),
            )
        }
        validateAddress(entity.address, "$path.address", errors)
        validatePhone(entity.phone, "$path.phone", errors)
        validateEmail(entity.email, "$path.email", errors)
        validateContactPerson(
            entity.representative,
            "$path.representative",
            "Osoba upoważniona do reprezentacji",
            errors,
        )
        validateContactPerson(entity.contactPerson, "$path.contactPerson", "Osoba do kontaktów roboczych", errors)
    }

    private fun validateNonFormalGroup(
        group: NonFormalGroupApplicantDto,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        if (group.partners.isEmpty() || group.partners.size > 5) {
            errors.add(
                FieldError(
                    "$path.partners",
                    FieldErrorCode.InvalidFormat,
                    "Grupa nieformalna musi liczyć od 1 do 5 partnerów",
                ),
            )
        }
        group.partners.forEachIndexed { index, partner ->
            when (partner) {
                is IndividualPartnerDto -> {
                    if (partner.firstName.isBlank()) {
                        errors.add(
                            FieldError(
                                "$path.partners[$index].firstName",
                                FieldErrorCode.Blank,
                                "Imię partnera jest wymagane",
                            ),
                        )
                    }
                    if (partner.lastName.isBlank()) {
                        errors.add(
                            FieldError(
                                "$path.partners[$index].lastName",
                                FieldErrorCode.Blank,
                                "Nazwisko partnera jest wymagane",
                            ),
                        )
                    }
                    validateAddress(partner.address, "$path.partners[$index].address", errors)
                    validatePhone(partner.phone, "$path.partners[$index].phone", errors)
                    validateEmail(partner.email, "$path.partners[$index].email", errors)
                }

                is EntityPartnerDto -> {
                    if (partner.name.isBlank()) {
                        errors.add(
                            FieldError(
                                "$path.partners[$index].name",
                                FieldErrorCode.Blank,
                                "Nazwa podmiotu partnerskiego jest wymagana",
                            ),
                        )
                    }
                    if (!isValidKrs(partner.krs)) {
                        errors.add(
                            FieldError(
                                "$path.partners[$index].krs",
                                FieldErrorCode.InvalidFormat,
                                "KRS partnera musi mieć 10 cyfr",
                            ),
                        )
                    }
                    if (!isValidRegon(partner.regon)) {
                        errors.add(
                            FieldError(
                                "$path.partners[$index].regon",
                                FieldErrorCode.InvalidFormat,
                                "Nieprawidłowy REGON partnera",
                            ),
                        )
                    }
                    if (!isValidNip(partner.nip)) {
                        errors.add(
                            FieldError(
                                "$path.partners[$index].nip",
                                FieldErrorCode.InvalidFormat,
                                "Nieprawidłowy NIP partnera",
                            ),
                        )
                    }
                    validateAddress(partner.address, "$path.partners[$index].address", errors)
                    validatePhone(partner.phone, "$path.partners[$index].phone", errors)
                    validateEmail(partner.email, "$path.partners[$index].email", errors)
                }
            }
        }
        validateContactPerson(group.representative, "$path.representative", "Reprezentant grupy do kontaktów", errors)
    }

    private fun validateAddress(
        address: AddressDto,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        if (address.street.isBlank()) {
            errors.add(FieldError("$path.street", FieldErrorCode.Blank, "Ulica jest wymagana"))
        }
        if (address.buildingNumber.isBlank()) {
            errors.add(FieldError("$path.buildingNumber", FieldErrorCode.Blank, "Numer budynku jest wymagany"))
        }
        if (!POSTAL_CODE_REGEX.matches(address.postalCode.trim())) {
            errors.add(
                FieldError(
                    "$path.postalCode",
                    FieldErrorCode.InvalidFormat,
                    "Kod pocztowy musi mieć format NN-NNN (np. 31-000)",
                ),
            )
        }
        if (address.city.isBlank()) {
            errors.add(FieldError("$path.city", FieldErrorCode.Blank, "Miejscowość jest wymagana"))
        }
    }

    private fun validateContactPerson(
        person: ContactPersonDto,
        path: String,
        label: String,
        errors: MutableList<FieldError>,
    ) {
        if (person.function.isBlank()) {
            errors.add(FieldError("$path.function", FieldErrorCode.Blank, "Funkcja ($label) jest wymagana"))
        }
        if (person.fullName.isBlank()) {
            errors.add(FieldError("$path.fullName", FieldErrorCode.Blank, "Imię i nazwisko ($label) są wymagane"))
        }
        validatePhone(person.phone, "$path.phone", errors)
        validateEmail(person.email, "$path.email", errors)
    }

    private fun validatePhone(
        phone: String,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        val digits = phone.filter { it.isDigit() }
        if (digits.length !in 7..15) {
            errors.add(
                FieldError(
                    path,
                    FieldErrorCode.InvalidFormat,
                    "Numer telefonu musi zawierać od 7 do 15 cyfr",
                ),
            )
        }
    }

    private fun validateEmail(
        email: String,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        if (!EMAIL_REGEX.matches(email.trim())) {
            errors.add(
                FieldError(
                    path,
                    FieldErrorCode.InvalidFormat,
                    "Nieprawidłowy format adresu e-mail",
                ),
            )
        }
    }

    private fun validatePlanAndBudget(
        plan: ActionPlanDto?,
        requestedAmount: Int?,
        errors: MutableList<FieldError>,
    ) {
        if (plan == null) {
            errors.add(FieldError("plan", FieldErrorCode.Blank, "Plan działania i koszty są wymagane"))
            return
        }

        if (plan.preparation.isEmpty()) {
            errors.add(
                FieldError(
                    "plan.preparation",
                    FieldErrorCode.Blank,
                    "Wymagana jest co najmniej jedna pozycja okresu przygotowawczego",
                ),
            )
        }
        if (plan.testingPhase1.isEmpty() && plan.testingPhase2.isEmpty()) {
            errors.add(
                FieldError(
                    "plan.testingPhase1",
                    FieldErrorCode.Blank,
                    "Wymagana jest co najmniej jedna pozycja okresu testowania",
                ),
            )
        }

        // Validate items in preparation
        plan.preparation.forEachIndexed { idx, item ->
            validatePlanItem(item, "plan.preparation[$idx]", errors)
        }
        plan.testingPhase1.forEachIndexed { idx, item ->
            validatePlanItem(item, "plan.testingPhase1[$idx]", errors)
        }
        plan.testingPhase2.forEachIndexed { idx, item ->
            validatePlanItem(item, "plan.testingPhase2[$idx]", errors)
        }

        // Validate duration limits
        val prepDuration = calculateDuration(plan.preparation)
        if (prepDuration > MAX_PREPARATION_MONTHS) {
            errors.add(
                FieldError(
                    "plan.preparation",
                    FieldErrorCode.InvalidFormat,
                    "Okres przygotowawczy nie może przekraczać $MAX_PREPARATION_MONTHS miesięcy (obecnie: $prepDuration)",
                ),
            )
        }

        val testDuration = calculateDuration(plan.testingPhase1 + plan.testingPhase2)
        if (testDuration > MAX_TESTING_MONTHS) {
            errors.add(
                FieldError(
                    "plan.testing",
                    FieldErrorCode.InvalidFormat,
                    "Łączny okres testowania (Faza I i II) nie może przekraczać $MAX_TESTING_MONTHS miesięcy (obecnie: $testDuration)",
                ),
            )
        }

        // Validate budget sum
        val totalCost = (plan.preparation + plan.testingPhase1 + plan.testingPhase2).sumOf { it.costGrosze }
        if (requestedAmount == null || requestedAmount <= 0) {
            errors.add(
                FieldError(
                    "requestedGrantAmountGrosze",
                    FieldErrorCode.Blank,
                    "Wnioskowana kwota grantu musi być większa od 0",
                ),
            )
        } else if (requestedAmount != totalCost) {
            errors.add(
                FieldError(
                    "requestedGrantAmountGrosze",
                    FieldErrorCode.InvalidFormat,
                    "Wnioskowana kwota grantu ($requestedAmount gr) musi być dokładnie równa sumie kosztów z planu działania ($totalCost gr)",
                ),
            )
        }
    }

    private fun validatePlanItem(
        item: PlanItemDto,
        path: String,
        errors: MutableList<FieldError>,
    ) {
        if (item.action.isBlank()) {
            errors.add(FieldError("$path.action", FieldErrorCode.Blank, "Opis działania jest wymagany"))
        }
        if (parseYearMonth(item.term) == null) {
            errors.add(
                FieldError("$path.term", FieldErrorCode.InvalidFormat, "Termin musi mieć format YYYY-MM (np. 2026-06)"),
            )
        }
        if (item.costGrosze < 0) {
            errors.add(FieldError("$path.costGrosze", FieldErrorCode.InvalidFormat, "Koszt nie może być ujemny"))
        }
    }

    private fun validateDeclarations(
        applicantType: ApplicantType?,
        acceptedDeclarations: List<String>,
        errors: MutableList<FieldError>,
    ) {
        if (applicantType == null) return
        val requiredIds = RopsDeclarations.getRequiredIds(applicantType)
        val missing = requiredIds - acceptedDeclarations.toSet()
        if (missing.isNotEmpty()) {
            errors.add(
                FieldError(
                    "declarations",
                    FieldErrorCode.InvalidFormat,
                    "Brak wymaganych oświadczeń: ${missing.joinToString(", ")}",
                ),
            )
        }
    }

    fun parseYearMonth(term: String): Pair<Int, Int>? {
        val parts = term.trim().split("-")
        if (parts.size != 2) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        if (month !in 1..12 || year !in 2020..2100) return null
        return year to month
    }

    fun calculateDuration(items: List<PlanItemDto>): Int {
        val terms = items.mapNotNull { parseYearMonth(it.term) }
        if (terms.isEmpty()) return 0
        val sorted = terms.sortedWith(compareBy({ it.first }, { it.second }))
        val first = sorted.first()
        val last = sorted.last()
        val startMonths = first.first * 12 + first.second
        val endMonths = last.first * 12 + last.second
        return endMonths - startMonths + 1
    }

    fun isValidNip(nip: String): Boolean {
        val clean = nip.filter { it.isDigit() }
        if (clean.length != 10) return false
        val digits = clean.map { it.digitToInt() }
        val weights = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        val sum = (0..8).sumOf { digits[it] * weights[it] }
        val mod = sum % 11
        return mod != 10 && mod == digits[9]
    }

    fun isValidRegon(regon: String): Boolean {
        val clean = regon.filter { it.isDigit() }
        val digits = clean.map { it.digitToInt() }
        return when (digits.size) {
            9 -> isValidRegon9(digits)
            14 -> isValidRegon9(digits.subList(0, 9)) && isValidRegon14(digits)
            else -> false
        }
    }

    private fun isValidRegon9(digits: List<Int>): Boolean {
        val weights = intArrayOf(8, 9, 2, 3, 4, 5, 6, 7)
        val sum = (0..7).sumOf { digits[it] * weights[it] }
        val checksum = (sum % 11) % 10
        return checksum == digits[8]
    }

    private fun isValidRegon14(digits: List<Int>): Boolean {
        val weights = intArrayOf(2, 4, 8, 5, 0, 9, 7, 3, 6, 1, 2, 4, 8)
        val sum = (0..12).sumOf { digits[it] * weights[it] }
        val checksum = (sum % 11) % 10
        return checksum == digits[13]
    }

    fun isValidKrs(krs: String): Boolean {
        val clean = krs.filter { it.isDigit() }
        return clean.length == 10
    }
}
