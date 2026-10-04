package io.github.mfabisiak.hubmi.web.mock

import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdminFeedbackDto
import io.github.mfabisiak.hubmi.api.AdminTestRequestDto
import io.github.mfabisiak.hubmi.api.ApplicationDto
import io.github.mfabisiak.hubmi.api.ChallengeDto
import io.github.mfabisiak.hubmi.api.GrantCallDto
import io.github.mfabisiak.hubmi.api.IdeaDto
import io.github.mfabisiak.hubmi.api.InnovationDto
import io.github.mfabisiak.hubmi.api.MaterialDto
import io.github.mfabisiak.hubmi.api.MessageDto
import io.github.mfabisiak.hubmi.api.NotificationDto
import io.github.mfabisiak.hubmi.api.SocialArea
import io.github.mfabisiak.hubmi.api.ThreadDto
import kotlinx.serialization.Serializable

/** The only signed-in user of the demo. */
internal const val DEMO_USER_ID = "demo-user"
internal const val DEMO_USER_NAME = "jan.kowalski"
internal const val DEMO_USER_EMAIL = "jan.kowalski@example.com"
internal const val DEMO_AUTHOR_NAME = "Jan Kowalski"
internal const val ROPS_TEAM_NAME = "Zespół Hubu Innowacji"

/** What the demo says wrote the plans of the Middleman: there is no model behind it. */
internal const val DEMO_MODEL_NAME = "bielik-demo (odpowiedzi przykładowe)"

@Serializable
internal data class MockIdea(
    val idea: IdeaDto,
    val authorId: String,
)

@Serializable
internal data class MockAdaptation(
    val adaptation: AdaptationDto,
    val authorId: String,
)

@Serializable
internal data class MockThread(
    val thread: ThreadDto,
    val messages: List<MessageDto>,
)

/** A problem described in the matchmaking form; what the trends and "similar needs" are made of. */
@Serializable
internal data class MockNeed(
    val id: String,
    val description: String,
    val municipality: String?,
    val area: SocialArea?,
    val matched: Boolean,
    val helpful: Boolean?,
    val createdAt: String,
)

/**
 * Everything the demo backend remembers. It is one immutable value, replaced as a whole by [MockStore.update] and kept
 * in the browser's `localStorage` as JSON, so what the visitor adds or changes survives a reload.
 */
@Serializable
internal data class MockDb(
    val innovations: List<InnovationDto>,
    val challenges: List<ChallengeDto>,
    val materials: List<MaterialDto>,
    val feedback: List<AdminFeedbackDto>,
    val testRequests: List<AdminTestRequestDto>,
    val ideas: List<MockIdea>,
    val calls: List<GrantCallDto>,
    val applications: List<ApplicationDto>,
    val adaptations: List<MockAdaptation>,
    val threads: List<MockThread>,
    val notifications: List<NotificationDto>,
    val needs: List<MockNeed>,
) {
    /** Ratings are derived from the feedback, so that a new rating shows up on the innovation at once. */
    fun innovationsWithRatings(): List<InnovationDto> =
        innovations.map { innovation ->
            val ratings = feedback.filter { it.innovationId == innovation.id }.map { it.rating }
            innovation.copy(
                averageRating = ratings.takeIf { it.isNotEmpty() }?.average(),
                ratingsCount = ratings.size,
            )
        }
}
