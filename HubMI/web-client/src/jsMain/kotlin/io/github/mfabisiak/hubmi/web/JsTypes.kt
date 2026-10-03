package io.github.mfabisiak.hubmi.web

import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.Page

/**
 * Types exported to TypeScript (this package and its sub-packages) may only use JS-friendly members: no value classes,
 * `Long`, `List` or `Map`. The Kotlin contract in `:core` is mapped onto them at the boundary; enums travel as names.
 */
@JsExport
class ApiErrorJs(
    val status: Int,
    /** An `ErrorCode` name from the server, or a client-side `NETWORK_ERROR`, `INVALID_RESPONSE`, `INVALID_ARGUMENT`, `HTTP_<status>`. */
    val code: String,
    val message: String,
    val details: Array<FieldErrorJs> = emptyArray(),
)

@JsExport
class FieldErrorJs(
    val field: String,
    /** `REQUIRED`, `BLANK`, `INVALID_FORMAT`, `MIN_VALUE` or `RANGE`. */
    val code: String,
    val message: String,
    /** Set for `MIN_VALUE` and `RANGE`. */
    val min: Int?,
    /** Set for `RANGE`. */
    val max: Int?,
)

/** Exceptions never cross the JS boundary: check [error] (or [ok]) instead of try/catch. */
@JsExport
class ApiResult<T>(
    val value: T?,
    val error: ApiErrorJs?,
) {
    val ok: Boolean get() = error == null
}

/** Value of an [ApiResult] for `204 No Content` responses; check [ApiResult.ok] only. */
@JsExport
class EmptyJs

@JsExport
class PageJs<T>(
    val items: Array<T>,
    val page: Int,
    val size: Int,
    val total: Int,
)

internal fun <A, B> Page<A>.toPageJs(transform: (A) -> B): PageJs<B> =
    PageJs(items.map(transform).toTypedArray(), page, size, total)

@JsExport
class HealthJs(
    val status: String,
)

@JsExport
class MeJs(
    val id: String?,
    val username: String?,
    val email: String?,
    /** `Role` names. */
    val roles: Array<String>,
)

internal fun ErrorResponse.toJs(status: Int): ApiErrorJs =
    ApiErrorJs(status, code.name, message, details.map { it.toJs() }.toTypedArray())

private fun FieldError.toJs(): FieldErrorJs =
    when (val reason = code) {
        FieldErrorCode.Required -> FieldErrorJs(field, "REQUIRED", message, null, null)
        FieldErrorCode.Blank -> FieldErrorJs(field, "BLANK", message, null, null)
        FieldErrorCode.InvalidFormat -> FieldErrorJs(field, "INVALID_FORMAT", message, null, null)
        is FieldErrorCode.MinValue -> FieldErrorJs(field, "MIN_VALUE", message, reason.min, null)
        is FieldErrorCode.Range -> FieldErrorJs(field, "RANGE", message, reason.min, reason.max)
    }
