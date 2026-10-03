package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
data class MeResponse(
    val id: String?,
    val username: String?,
    val email: String?,
    val roles: Set<Role>,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class FieldError(
    val field: String,
    val code: FieldErrorCode,
    val message: String,
)

@Serializable
data class ErrorResponse(
    val code: ErrorCode,
    val message: String,
    val details: List<FieldError> = emptyList(),
)
