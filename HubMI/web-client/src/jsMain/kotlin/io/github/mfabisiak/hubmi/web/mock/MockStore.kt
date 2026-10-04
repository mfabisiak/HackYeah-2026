package io.github.mfabisiak.hubmi.web.mock

import arrow.core.Either
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/** Bumped whenever [MockDb] changes shape, so that data saved by an older build is not read as the new one. */
private const val STORAGE_KEY = "hubmi.demo.db.v1"

private val json = Json { ignoreUnknownKeys = true }

internal external interface KeyValueStorage {
    fun getItem(key: String): String?

    fun setItem(
        key: String,
        value: String,
    )

    fun removeItem(key: String)
}

internal external val localStorage: KeyValueStorage

/** The browser may refuse storage altogether (private mode, blocked cookies): the demo then just starts afresh. */
internal fun readSavedDb(): MockDb? =
    Either
        .catch { localStorage.getItem(STORAGE_KEY) }
        .getOrNull()
        ?.let { text -> Either.catch { json.decodeFromString<MockDb>(text) }.getOrNull() }

private fun saveDb(db: MockDb) {
    Either.catch { localStorage.setItem(STORAGE_KEY, json.encodeToString(MockDb.serializer(), db)) }
}

internal fun forgetSavedDb() {
    Either.catch { localStorage.removeItem(STORAGE_KEY) }
}

/** Holds the demo's data; every change is saved to `localStorage` right away. */
internal class MockStore(
    initial: MockDb,
    private val persist: (MockDb) -> Unit = ::saveDb,
) {
    private val state = MutableStateFlow(initial)

    val db: MockDb get() = state.value

    fun update(transform: (MockDb) -> MockDb) {
        state.update(transform)
        persist(state.value)
    }
}
