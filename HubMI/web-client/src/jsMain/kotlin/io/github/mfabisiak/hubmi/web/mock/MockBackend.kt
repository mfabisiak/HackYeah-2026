package io.github.mfabisiak.hubmi.web.mock

import arrow.core.raise.Raise
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ErrorCode
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.StreamJs
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.startStream
import io.github.mfabisiak.hubmi.web.toJs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.js.Date
import kotlin.js.Promise
import kotlin.random.Random

/** A pause that makes the answers look like they came over the network. */
private const val LATENCY_MS = 250L
private const val DAY_MS = 86_400_000.0

/** Runs the calls of the demo: every answer arrives after a short delay, as a failure or a value, never as an exception. */
internal class MockBackend(
    val store: MockStore,
    initialAccount: DemoAccount,
    private val scope: CoroutineScope = MainScope(),
) {
    private val signedIn = MutableStateFlow(initialAccount)

    val account: DemoAccount get() = signedIn.value

    fun signIn(account: DemoAccount) {
        signedIn.value = account
        saveAccount(account)
    }

    fun <T> respond(block: suspend Raise<ApiErrorJs>.() -> T): Promise<ApiResult<T>> =
        scope.promiseResult {
            delay(LATENCY_MS)
            either { block() }
        }

    fun <A, B> stream(
        call: suspend Raise<ApiErrorJs>.() -> A,
        transform: (A) -> B,
    ): StreamJs<B> = scope.startStream(call = { either { call() } }, transform = transform)
}

internal fun notFound(what: String): ApiErrorJs =
    ApiErrorJs(STATUS_NOT_FOUND, ErrorCode.NOT_FOUND.name, "Nie znaleziono: $what")

internal fun conflict(message: String): ApiErrorJs = ApiErrorJs(STATUS_CONFLICT, ErrorCode.CONFLICT.name, message)

internal fun forbidden(message: String): ApiErrorJs = ApiErrorJs(STATUS_FORBIDDEN, ErrorCode.FORBIDDEN.name, message)

internal fun invalid(vararg errors: FieldError): ApiErrorJs =
    ApiErrorJs(
        STATUS_BAD_REQUEST,
        ErrorCode.VALIDATION_FAILED.name,
        "Błąd walidacji",
        errors.map { it.toJs() }.toTypedArray(),
    )

internal fun blank(field: String): FieldError = FieldError(field, FieldErrorCode.Blank, "Pole nie może być puste")

private const val STATUS_BAD_REQUEST = 400
private const val STATUS_FORBIDDEN = 403
private const val STATUS_NOT_FOUND = 404
private const val STATUS_CONFLICT = 409

internal fun <T> List<T>.pageOf(
    page: Int,
    size: Int,
): Page<T> = Page(items = drop(page * size).take(size), page = page, size = size, total = this.size)

internal fun newId(prefix: String): String = "$prefix-${Random.nextInt(Int.MAX_VALUE).toString(RADIX)}"

private const val RADIX = 36

internal fun isoDaysFromNow(days: Int): String = Date(Date.now() + days * DAY_MS).toISOString()

internal fun nowIso(): String = isoDaysFromNow(0)

internal fun epochMillis(iso: String): Double = Date(iso).getTime()
