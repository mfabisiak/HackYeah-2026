package io.github.mfabisiak.hubmi.web

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

/** Status of errors that never reached the server (network failure, invalid argument passed from JS). */
internal const val CLIENT_ERROR_STATUS = 0

/** Runs [request] and maps every failure (network, non-2xx HTTP) to [ApiErrorJs] without throwing. */
internal suspend fun HttpClient.execute(
    request: suspend HttpClient.() -> HttpResponse,
): Either<ApiErrorJs, HttpResponse> =
    Either
        .catch { request() }
        .mapLeft { ApiErrorJs(CLIENT_ERROR_STATUS, "NETWORK_ERROR", it.message ?: "Network error") }
        .flatMap { response ->
            if (response.status.isSuccess()) response.right() else response.toApiError().left()
        }

private suspend fun HttpResponse.toApiError(): ApiErrorJs =
    Either
        .catch { body<ErrorResponse>() }
        .fold(
            ifLeft = { ApiErrorJs(status.value, "HTTP_${status.value}", status.description) },
            ifRight = { it.toJs(status.value) },
        )

internal suspend inline fun <reified T> Either<ApiErrorJs, HttpResponse>.decode(): Either<ApiErrorJs, T> =
    flatMap { response ->
        Either
            .catch { response.body<T>() }
            .mapLeft { ApiErrorJs(response.status.value, "INVALID_RESPONSE", it.message ?: "Invalid response") }
    }

/** Typed GET of a shared `@Resource`. */
internal suspend inline fun <reified R : Any, reified T> HttpClient.fetch(resource: R): Either<ApiErrorJs, T> =
    execute { get(resource) }.decode()

/** Typed request with a JSON [body] and a JSON response. */
internal suspend inline fun <reified R : Any, reified B : Any, reified T> HttpClient.send(
    method: HttpMethod,
    resource: R,
    body: B,
): Either<ApiErrorJs, T> =
    execute {
        request(resource) {
            this.method = method
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }.decode()

/** Typed request with a JSON [body] and an empty (`204`) response. */
internal suspend inline fun <reified R : Any, reified B : Any> HttpClient.sendForUnit(
    method: HttpMethod,
    resource: R,
    body: B,
): Either<ApiErrorJs, Unit> =
    execute {
        request(resource) {
            this.method = method
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }.map { }

/** Typed request without a body and with an empty (`204`) response. */
internal suspend inline fun <reified R : Any> HttpClient.sendForUnit(
    method: HttpMethod,
    resource: R,
): Either<ApiErrorJs, Unit> = execute { request(resource) { this.method = method } }.map { }

/** Typed request without a body and with a JSON response. */
internal suspend inline fun <reified R : Any, reified T> HttpClient.sendWithoutBody(
    method: HttpMethod,
    resource: R,
): Either<ApiErrorJs, T> = execute { request(resource) { this.method = method } }.decode()

internal fun <A, B> Either<ApiErrorJs, A>.toResult(transform: (A) -> B): ApiResult<B> =
    fold(
        ifLeft = { ApiResult(null, it) },
        ifRight = { ApiResult(transform(it), null) },
    )

/** Bridges a suspending call to a JS `Promise` that always resolves to an [ApiResult]. */
internal fun <T> CoroutineScope.promiseResult(block: suspend () -> Either<ApiErrorJs, T>): Promise<ApiResult<T>> =
    promise { block().toResult { it } }
