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
        /** Roles of ROPS officials: they moderate submissions and answer residents in the name of ROPS. */
        val staff: Set<Role> = setOf(ADMIN, EXPERT)

        fun fromKeycloakName(name: String): Role? = entries.firstOrNull { it.keycloakName == name }
    }
}

/** Whether the user is a ROPS official, i.e. holds any of [Role.staff]. */
fun Set<Role>.isStaff(): Boolean = any(Role.staff::contains)
