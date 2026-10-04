package io.github.mfabisiak.hubmi.web.matching

import io.github.mfabisiak.hubmi.api.MatchFeedbackRequest
import io.github.mfabisiak.hubmi.api.MatchRequest
import io.github.mfabisiak.hubmi.api.MatchResult
import io.github.mfabisiak.hubmi.api.Matches
import io.github.mfabisiak.hubmi.web.ApiResult
import io.github.mfabisiak.hubmi.web.EmptyJs
import io.github.mfabisiak.hubmi.web.promiseResult
import io.github.mfabisiak.hubmi.web.send
import io.github.mfabisiak.hubmi.web.sendForUnit
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlin.js.Promise

internal class HttpMatchesApi(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) : MatchesApi {
    override fun match(
        description: String,
        municipality: String?,
    ): Promise<ApiResult<MatchResultJs>> =
        scope.promiseResult {
            client
                .send<Matches, MatchRequest, MatchResult>(
                    HttpMethod.Post,
                    Matches(),
                    MatchRequest(description, municipality),
                ).map { it.toJs() }
        }

    override fun sendFeedback(
        needId: String,
        helpful: Boolean,
    ): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult {
            client
                .sendForUnit(HttpMethod.Put, Matches.Feedback(needId = needId), MatchFeedbackRequest(helpful))
                .map { EmptyJs() }
        }
}
