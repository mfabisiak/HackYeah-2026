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

/** `/api/matches`: public, login optional. */
@JsExport
class MatchesApi internal constructor(
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    /** Describes a problem and returns matching innovations. */
    fun match(
        description: String,
        municipality: String? = null,
    ): Promise<ApiResult<MatchResultJs>> =
        scope.promiseResult {
            client
                .send<Matches, MatchRequest, MatchResult>(
                    HttpMethod.Post,
                    Matches(),
                    MatchRequest(description, municipality),
                ).map { it.toJs() }
        }

    /** "Did this help?" for the need created by [match]. */
    fun sendFeedback(
        needId: String,
        helpful: Boolean,
    ): Promise<ApiResult<EmptyJs>> =
        scope.promiseResult {
            client
                .sendForUnit(HttpMethod.Post, Matches.Feedback(needId = needId), MatchFeedbackRequest(helpful))
                .map { EmptyJs() }
        }
}
