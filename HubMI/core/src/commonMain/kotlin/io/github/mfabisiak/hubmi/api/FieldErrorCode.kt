package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Machine-readable reason of a single field validation problem (`FieldError.code`). Codes that depend on a limit carry
 * it as data, so clients can build their own message instead of parsing the Polish one.
 */
@Serializable
sealed interface FieldErrorCode {
    @Serializable
    @SerialName("REQUIRED")
    data object Required : FieldErrorCode

    @Serializable
    @SerialName("BLANK")
    data object Blank : FieldErrorCode

    @Serializable
    @SerialName("INVALID_FORMAT")
    data object InvalidFormat : FieldErrorCode

    @Serializable
    @SerialName("MIN_VALUE")
    data class MinValue(
        val min: Int,
    ) : FieldErrorCode

    @Serializable
    @SerialName("RANGE")
    data class Range(
        val min: Int,
        val max: Int,
    ) : FieldErrorCode
}
