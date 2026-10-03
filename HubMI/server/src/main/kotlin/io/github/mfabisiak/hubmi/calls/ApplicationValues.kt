package io.github.mfabisiak.hubmi.calls

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode

@JvmInline
value class Nip private constructor(
    val value: String,
) {
    companion object {
        private val WEIGHTS = intArrayOf(6, 5, 7, 2, 3, 4, 5, 6, 7)

        fun parse(
            field: String,
            raw: String?,
        ): Either<FieldError, Nip> =
            either {
                ensure(!raw.isNullOrBlank()) {
                    FieldError(field, FieldErrorCode.Blank, "NIP jest wymagany")
                }
                val cleaned = raw.filter { it != '-' && it != ' ' }
                ensure(cleaned.length == 10 && cleaned.all { it in '0'..'9' }) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "NIP musi składać się z 10 cyfr")
                }
                val digits = cleaned.map { it.digitToInt() }
                val checksum = (0 until 9).sumOf { digits[it] * WEIGHTS[it] } % 11
                ensure(checksum != 10 && checksum == digits[9]) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "Nieprawidłowa suma kontrolna NIP")
                }
                Nip(cleaned)
            }
    }
}

@JvmInline
value class Regon private constructor(
    val value: String,
) {
    companion object {
        private val WEIGHTS_9 = intArrayOf(8, 9, 2, 3, 4, 5, 6, 7)
        private val WEIGHTS_14 = intArrayOf(2, 4, 8, 5, 0, 9, 7, 3, 6, 1, 2, 4, 8)

        fun parse(
            field: String,
            raw: String?,
        ): Either<FieldError, Regon> =
            either {
                ensure(!raw.isNullOrBlank()) {
                    FieldError(field, FieldErrorCode.Blank, "REGON jest wymagany")
                }
                val cleaned = raw.filter { it != '-' && it != ' ' }
                ensure(cleaned.all { it in '0'..'9' } && (cleaned.length == 9 || cleaned.length == 14)) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "REGON musi składać się z 9 lub 14 cyfr")
                }
                val digits = cleaned.map { it.digitToInt() }
                val checksum9 = ((0 until 8).sumOf { digits[it] * WEIGHTS_9[it] } % 11) % 10
                ensure(checksum9 == digits[8]) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "Nieprawidłowa suma kontrolna REGON")
                }
                if (cleaned.length == 14) {
                    val checksum14 = ((0 until 13).sumOf { digits[it] * WEIGHTS_14[it] } % 11) % 10
                    ensure(checksum14 == digits[13]) {
                        FieldError(field, FieldErrorCode.InvalidFormat, "Nieprawidłowa suma kontrolna REGON-14")
                    }
                }
                Regon(cleaned)
            }
    }
}

@JvmInline
value class Krs private constructor(
    val value: String,
) {
    companion object {
        fun parse(
            field: String,
            raw: String?,
        ): Either<FieldError, Krs> =
            either {
                ensure(!raw.isNullOrBlank()) {
                    FieldError(field, FieldErrorCode.Blank, "Numer KRS jest wymagany")
                }
                val cleaned = raw.trim()
                ensure(cleaned.all { it in '0'..'9' } && cleaned.length == 10) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "Numer KRS musi składać się z 10 cyfr")
                }
                Krs(cleaned)
            }
    }
}

@JvmInline
value class PostalCode private constructor(
    val value: String,
) {
    companion object {
        private val REGEX = Regex("""^[0-9]{2}-[0-9]{3}$""")

        fun parse(
            field: String,
            raw: String?,
        ): Either<FieldError, PostalCode> =
            either {
                ensure(!raw.isNullOrBlank()) {
                    FieldError(field, FieldErrorCode.Blank, "Kod pocztowy jest wymagany")
                }
                val trimmed = raw.trim()
                ensure(REGEX.matches(trimmed)) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "Kod pocztowy musi mieć format XX-XXX")
                }
                PostalCode(trimmed)
            }
    }
}

@JvmInline
value class Email private constructor(
    val value: String,
) {
    companion object {
        private val REGEX = Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

        fun parse(
            field: String,
            raw: String?,
        ): Either<FieldError, Email> =
            either {
                ensure(!raw.isNullOrBlank()) {
                    FieldError(field, FieldErrorCode.Blank, "Adres e-mail jest wymagany")
                }
                val trimmed = raw.trim()
                ensure(REGEX.matches(trimmed)) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "Nieprawidłowy format adresu e-mail")
                }
                Email(trimmed)
            }
    }
}

@JvmInline
value class Phone private constructor(
    val value: String,
) {
    companion object {
        private val REGEX = Regex("""^\+?[0-9\s\-]{9,18}$""")

        fun parse(
            field: String,
            raw: String?,
        ): Either<FieldError, Phone> =
            either {
                ensure(!raw.isNullOrBlank()) {
                    FieldError(field, FieldErrorCode.Blank, "Numer telefonu jest wymagany")
                }
                val trimmed = raw.trim()
                val digitsCount = trimmed.count { it in '0'..'9' }
                ensure(REGEX.matches(trimmed) && digitsCount in 9..15) {
                    FieldError(field, FieldErrorCode.InvalidFormat, "Nieprawidłowy format numeru telefonu")
                }
                Phone(trimmed)
            }
    }
}
