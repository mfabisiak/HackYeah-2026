package io.github.mfabisiak.hubmi.common.http

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.common.DomainError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun ApplicationCall.respondError(error: DomainError) {
    val (status, response) =
        when (error) {
            is DomainError.NotFound -> {
                HttpStatusCode.NotFound to ErrorResponse(error.code, error.message)
            }

            is DomainError.Conflict -> {
                HttpStatusCode.Conflict to ErrorResponse(error.code, error.message)
            }

            is DomainError.Validation -> {
                HttpStatusCode.BadRequest to ErrorResponse(error.code, error.message, error.details)
            }

            is DomainError.Unauthorized -> {
                HttpStatusCode.Unauthorized to ErrorResponse(error.code, error.message)
            }

            is DomainError.Forbidden -> {
                HttpStatusCode.Forbidden to ErrorResponse(error.code, error.message)
            }

            is DomainError.Unavailable -> {
                HttpStatusCode.ServiceUnavailable to ErrorResponse(error.code, error.message)
            }

            is DomainError.Internal -> {
                HttpStatusCode.InternalServerError to ErrorResponse(error.code, error.message)
            }
        }
    respond(status, response)
}

suspend fun RoutingContext.respondError(error: DomainError) = call.respondError(error)

suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: Either<DomainError, T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
) {
    result.fold(
        ifLeft = { respondError(it) },
        ifRight = { respond(successStatus, it) },
    )
}

suspend inline fun <reified T : Any> RoutingContext.respondResult(
    result: Either<DomainError, T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
) = call.respondResult(result, successStatus)

suspend inline fun <reified T : Any> ApplicationCall.respondEither(
    either: Either<DomainError, T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
) = respondResult(either, successStatus)

suspend inline fun <reified T : Any> RoutingContext.respondEither(
    either: Either<DomainError, T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
) = call.respondResult(either, successStatus)

suspend fun ApplicationCall.respondResultUnit(
    result: Either<DomainError, Unit>,
    successStatus: HttpStatusCode = HttpStatusCode.NoContent,
) {
    result.fold(
        ifLeft = { respondError(it) },
        ifRight = { respond(successStatus) },
    )
}

suspend fun RoutingContext.respondResultUnit(
    result: Either<DomainError, Unit>,
    successStatus: HttpStatusCode = HttpStatusCode.NoContent,
) = call.respondResultUnit(result, successStatus)

suspend fun ApplicationCall.respondEitherUnit(
    either: Either<DomainError, Unit>,
    successStatus: HttpStatusCode = HttpStatusCode.NoContent,
) = respondResultUnit(either, successStatus)

suspend fun RoutingContext.respondEitherUnit(
    either: Either<DomainError, Unit>,
    successStatus: HttpStatusCode = HttpStatusCode.NoContent,
) = call.respondResultUnit(either, successStatus)

/** Runs [block] (parse the request, call the service) and responds with its result or the first domain error. */
suspend inline fun <reified T : Any> RoutingContext.respondEither(
    successStatus: HttpStatusCode = HttpStatusCode.OK,
    block: Raise<DomainError>.() -> T,
) = respondEither(either(block), successStatus)

suspend inline fun RoutingContext.respondEitherUnit(block: Raise<DomainError>.() -> Unit) =
    respondEitherUnit(either(block))
