package io.github.mfabisiak.hubmi.auth

import io.github.mfabisiak.hubmi.api.MeResponse
import io.ktor.server.auth.jwt.*

fun JWTPrincipal.toMeResponse(): MeResponse =
    MeResponse(
        id = subject,
        username = payload.getClaim("preferred_username").asString(),
        email = payload.getClaim("email").asString(),
        roles = realmRoles,
    )
