package io.github.mfabisiak.hubmi.auth

/** Keycloak subject (`sub`) of the authenticated user. */
@JvmInline
value class UserId(
    val value: String,
)
