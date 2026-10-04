package io.github.mfabisiak.hubmi.web.adaptations

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationEvent
import io.github.mfabisiak.hubmi.api.AdaptationResponse
import io.github.mfabisiak.hubmi.api.Adaptations
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.SseMessage
import io.github.mfabisiak.hubmi.web.ApiErrorJs
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.PageJs
import io.github.mfabisiak.hubmi.web.StreamJs
import io.github.mfabisiak.hubmi.web.callSafely
import io.github.mfabisiak.hubmi.web.collectEvents
import io.github.mfabisiak.hubmi.web.decoder
import io.github.mfabisiak.hubmi.web.eventStream
import io.github.mfabisiak.hubmi.web.fetch
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.startStream
import io.github.mfabisiak.hubmi.web.toPageJs
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.js.Promise

internal class HttpAdaptationsApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : AdaptationsApi {
    override fun request(
        innovationId: String,
        institution: InstitutionJs,
        listener: AdaptationListenerJs?,
    ): StreamJs<AdaptationResponseJs> =
        scope.startStream(
            call = {
                either {
                    val resource = Innovations.ById.Adaptations(Innovations.ById(id = innovationId))
                    answer(client.eventStream(resource, institution.toDto().bind()), listener).bind()
                }
            },
            transform = AdaptationResponse::toJs,
        )

    override fun mine(
        page: Int,
        size: Int,
    ): Promise<ApiResult<PageJs<AdaptationJs>>> =
        scope.promiseResult {
            client
                .fetch<Adaptations.Mine, Page<AdaptationDto>>(Adaptations.Mine(page = page, size = size))
                .map { it.toPageJs { adaptation -> adaptation.toJs() } }
        }

    override fun get(id: String): Promise<ApiResult<AdaptationJs>> =
        scope.promiseResult {
            client.fetch<Adaptations.ById, AdaptationDto>(Adaptations.ById(id = id)).map { it.toJs() }
        }

    private suspend fun answer(
        messages: Flow<Either<ApiErrorJs, SseMessage>>,
        listener: AdaptationListenerJs?,
    ): Either<ApiErrorJs, AdaptationResponse> =
        messages.collectEvents(
            decode = AdaptationEvent.serializer().decoder(),
            onEvent = { event -> listener?.deliver(event) },
            finish = { event ->
                when (event) {
                    is AdaptationEvent.Done -> event.response.right()
                    is AdaptationEvent.Failed -> event.error.toApiError().left()
                    is AdaptationEvent.Step -> null
                }
            },
        )
}

internal fun AdaptationListenerJs.deliver(event: AdaptationEvent) {
    if (event is AdaptationEvent.Step) {
        onStep?.let { callback -> callSafely { callback(event.step.toJs()) } }
    }
}

/** The server fault that stopped the stream, as an error of the call. */
private fun ErrorResponse.toApiError(): ApiErrorJs = ApiErrorJs(SERVER_ERROR_STATUS, code.name, message)

private const val SERVER_ERROR_STATUS = 500
