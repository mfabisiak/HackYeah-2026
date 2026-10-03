package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.Serializable

/** Stable machine-readable identifier of an API error (`ErrorResponse.code`). */
@Serializable
enum class ErrorCode {
    NOT_FOUND,
    CONFLICT,
    VALIDATION_FAILED,
    UNAUTHORIZED,
    FORBIDDEN,
    SERVICE_UNAVAILABLE,
    INTERNAL_ERROR,
    NOT_IMPLEMENTED,
}
