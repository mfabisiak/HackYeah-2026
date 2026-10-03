package io.github.mfabisiak.hubmi.common

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import java.net.URI

@JvmInline
value class HttpUrl private constructor(
    val value: String,
) {
    companion object {
        private val LENGTHS = 1..2048
        private val SCHEMES = setOf("http", "https")

        fun parse(
            raw: String,
            field: String,
        ): Either<FieldError, HttpUrl> =
            either {
                val text = parseText(raw, field, LENGTHS).bind()
                val invalid = FieldError(field, FieldErrorCode.InvalidFormat, "Pole '$field' musi być adresem http(s)")
                val uri = ensureNotNull(Either.catch { URI(text) }.getOrNull()) { invalid }
                ensure(uri.scheme?.lowercase() in SCHEMES && !uri.host.isNullOrBlank()) { invalid }
                HttpUrl(text)
            }
    }
}
