package io.github.mfabisiak.hubmi.service.matching

/**
 * Replaces e-mail addresses, PESEL numbers and phone numbers with placeholders that keep the sentence readable.
 * Names and addresses cannot be detected without NER, so the UI has to warn users not to type them.
 */
object PersonalDataScrubber {
    private const val EMAIL_PLACEHOLDER = "[EMAIL]"
    private const val PESEL_PLACEHOLDER = "[PESEL]"
    private const val PHONE_PLACEHOLDER = "[TELEFON]"

    private val EMAIL = Regex("""[\p{L}\p{N}._%+-]+@[\p{L}\p{N}-]+(?:\.[\p{L}\p{N}-]+)*\.\p{L}{2,}""")
    private val PESEL = Regex("""(?<!\d)\d{11}(?!\d)""")
    private val PHONE =
        Regex(
            """(?<![\d+])(?:(?:\+|00)?48[ -]?)?(?:\d{3}[ -]?\d{3}[ -]?\d{3}|\d{2}[ -]\d{3}[ -]\d{2}[ -]\d{2})(?!\d)""",
        )

    private val PESEL_WEIGHTS = listOf(1, 3, 7, 9, 1, 3, 7, 9, 1, 3)

    fun scrub(text: String): String =
        text
            .replace(EMAIL, EMAIL_PLACEHOLDER)
            .replace(PESEL) { if (isValidPesel(it.value)) PESEL_PLACEHOLDER else it.value }
            .replace(PHONE, PHONE_PLACEHOLDER)

    /** Eleven digits whose last digit is the weighted checksum of the first ten (the date part is not checked). */
    fun isValidPesel(candidate: String): Boolean {
        val digits = candidate.mapNotNull(Char::digitToIntOrNull)
        val checksum = PESEL_WEIGHTS.zip(digits).sumOf { (weight, digit) -> weight * digit }
        return digits.size == PESEL_WEIGHTS.size + 1 && (10 - checksum % 10) % 10 == digits.last()
    }
}
