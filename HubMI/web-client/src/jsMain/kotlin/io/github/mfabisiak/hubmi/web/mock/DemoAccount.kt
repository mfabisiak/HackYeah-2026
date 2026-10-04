package io.github.mfabisiak.hubmi.web.mock

import arrow.core.Either
import io.github.mfabisiak.hubmi.api.ParticipantRole
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.web.MeJs

private const val ACCOUNT_KEY = "hubmi.demo.account.v1"

/** The accounts a visitor of the demo can sign in as, one per role of the realm; they all see the same data. */
internal enum class DemoAccount(
    val label: String,
    val username: String,
    val displayName: String,
    val roles: List<Role>,
    val participantRole: ParticipantRole,
) {
    USER("Użytkownik", DEMO_USER_NAME, DEMO_AUTHOR_NAME, listOf(Role.USER), ParticipantRole.AUTHOR),
    EXPERT("Ekspert", "anna.nowak", "Anna Nowak", listOf(Role.USER, Role.EXPERT), ParticipantRole.EXPERT),
    ADMIN(
        "Administrator",
        "piotr.wisniewski",
        "Piotr Wiśniewski",
        listOf(Role.USER, Role.ADMIN),
        ParticipantRole.ADMIN,
    ),
    ;

    fun toMe(): MeJs =
        MeJs(DEMO_USER_ID, username, "$username@example.com", roles.map { it.keycloakName }.toTypedArray())
}

internal fun readSavedAccount(): DemoAccount =
    Either
        .catch { localStorage.getItem(ACCOUNT_KEY) }
        .getOrNull()
        ?.let { name -> DemoAccount.entries.firstOrNull { it.name == name } }
        ?: DemoAccount.USER

internal fun saveAccount(account: DemoAccount) {
    Either.catch { localStorage.setItem(ACCOUNT_KEY, account.name) }
}
