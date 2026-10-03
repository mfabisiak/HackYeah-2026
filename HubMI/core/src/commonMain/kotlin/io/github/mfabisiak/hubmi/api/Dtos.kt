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
    val roles: Set<String>,
)

@Serializable
data class MessageResponse(
    val message: String,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)
