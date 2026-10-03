package io.github.mfabisiak.hubmi.api

import kotlinx.serialization.Serializable

/** Keycloak realm roles recognised by the platform; [keycloakName] is the role name in the `realm_access` claim. */
@Serializable
enum class Role(
    val keycloakName: String,
) {
    USER("user"),
    EXPERT("expert"),
    ADMIN("admin"),
    ;

    companion object {
        fun fromKeycloakName(name: String): Role? = entries.firstOrNull { it.keycloakName == name }
    }
}
