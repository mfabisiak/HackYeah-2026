package io.github.mfabisiak.hubmi.web

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess

internal const val NETWORK_ERROR_STATUS = 0

/** Performs a typed GET and maps every failure (network, HTTP, decoding) to [ApiErrorJs] without throwing. */
internal suspend inline fun <reified R : Any, reified T> HttpClient.fetch(resource: R): Either<ApiErrorJs, T> =
    Either
        .catch { get(resource) }
        .mapLeft { ApiErrorJs(NETWORK_ERROR_STATUS, "network_error", it.message ?: "Network error") }
        .flatMap { response ->
            if (response.status.isSuccess()) {
                Either
                    .catch { response.body<T>() }
                    .mapLeft {
                        ApiErrorJs(response.status.value, "invalid_response", it.message ?: "Invalid response")
                    }
            } else {
                Either
                    .catch { response.body<ErrorResponse>() }
                    .fold(
                        ifLeft = {
                            ApiErrorJs(
                                response.status.value,
                                "http_${response.status.value}",
                                response.status.description,
                            )
                        },
                        ifRight = { ApiErrorJs(response.status.value, it.code.name, it.message) },
                    ).left()
            }
        }

internal fun <A, B> Either<ApiErrorJs, A>.toResult(transform: (A) -> B): ApiResult<B> =
    fold(
        ifLeft = { ApiResult(null, it) },
        ifRight = { ApiResult(transform(it), null) },
    )
