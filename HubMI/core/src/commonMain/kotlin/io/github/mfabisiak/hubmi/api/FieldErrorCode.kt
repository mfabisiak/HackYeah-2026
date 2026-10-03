package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.Serializable

/** Stable machine-readable identifier of a single field validation problem (`FieldError.code`). */
@Serializable
enum class FieldErrorCode {
    REQUIRED,
    BLANK,
    INVALID_FORMAT,
    MIN_VALUE,
    RANGE,
}
