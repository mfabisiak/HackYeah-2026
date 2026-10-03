package io.github.mfabisiak.hubmi.web

/**
 * Types exported to TypeScript. Only JS-friendly members are allowed here (no value classes, `Long`, `List`),
 * so the Kotlin contract in `:core` is mapped onto these at the boundary.
 */
@JsExport
class ApiErrorJs(
    val status: Int,
    val code: String,
    val message: String,
)

/** Exceptions never cross the JS boundary: check [error] (or [ok]) instead of try/catch. */
@JsExport
class ApiResult<T>(
    val value: T?,
    val error: ApiErrorJs?,
) {
    val ok: Boolean get() = error == null
}

@JsExport
class HealthJs(
    val status: String,
)

@JsExport
class MeJs(
    val id: String?,
    val username: String?,
    val email: String?,
    val roles: Array<String>,
)
