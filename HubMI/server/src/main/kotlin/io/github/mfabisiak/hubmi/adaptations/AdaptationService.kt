package io.github.mfabisiak.hubmi.adaptations

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.mfabisiak.hubmi.api.AdaptationDto
import io.github.mfabisiak.hubmi.api.AdaptationEvent
import io.github.mfabisiak.hubmi.api.AdaptationResponse
import io.github.mfabisiak.hubmi.api.AdaptationStatus
import io.github.mfabisiak.hubmi.api.AiStatus
import io.github.mfabisiak.hubmi.api.ErrorResponse
import io.github.mfabisiak.hubmi.api.FieldError
import io.github.mfabisiak.hubmi.api.FieldErrorCode
import io.github.mfabisiak.hubmi.api.Page
import io.github.mfabisiak.hubmi.api.PageRequest
import io.github.mfabisiak.hubmi.assistant.LlmClient
import io.github.mfabisiak.hubmi.assistant.LlmSlots
import io.github.mfabisiak.hubmi.assistant.StreamedAnswer
import io.github.mfabisiak.hubmi.assistant.aiStatus
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.common.DomainError
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.github.mfabisiak.hubmi.innovations.InnovationItem
import io.github.mfabisiak.hubmi.innovations.InnovationRepository
import io.github.mfabisiak.hubmi.innovations.innovationNotFound
import io.github.mfabisiak.hubmi.matching.PersonalDataScrubber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import org.bson.types.ObjectId
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The Middleman: asks the local model how a library innovation could be run by a given institution and keeps the plan
 * for an admin to review. A plan is stored only when the model wrote one that passes [AdaptationParser], so what is
 * stored is whole; a model that is off, busy, slow or incoherent is the `aiStatus` of the answer, not an error.
 */
class AdaptationService(
    private val innovations: InnovationRepository,
    private val adaptations: AdaptationRepository,
    private val llm: LlmClient,
    private val slots: LlmSlots,
    private val settings: Settings,
    private val clock: Clock = Clock.systemUTC(),
) {
    /** [model] is recorded with each plan; [deadline] bounds the generation including the wait for the model. */
    data class Settings(
        val enabled: Boolean,
        val model: String,
        val deadline: Duration = DEFAULT_DEADLINE,
    )

    /**
     * Checks that the innovation exists (the only failure that is an error before the stream starts) and returns the
     * plan as events: an [AdaptationEvent.Step] for each step as the model writes it, then [AdaptationEvent.Done] with
     * the stored plan, or [AdaptationEvent.Failed] if storing it failed.
     */
    suspend fun adapt(
        innovationId: InnovationId,
        author: UserId,
        draft: InstitutionDraft,
    ): Either<DomainError, Flow<AdaptationEvent>> =
        either {
            val innovation =
                ensureNotNull(innovations.findById(innovationId).mapLeft { it.toDomainError() }.bind()) {
                    innovationNotFound(innovationId)
                }
            val institution = draft.toInstitution().let { it.copy(context = PersonalDataScrubber.scrub(it.context)) }
            flow {
                val answer =
                    if (settings.enabled) slots.within(settings.deadline) { consult(innovation, institution) } else null
                emit(conclude(answer, innovation, author, institution))
            }
        }

    /** The plans of [author], newest first. */
    suspend fun mine(
        author: UserId,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<AdaptationDto>> =
        either {
            adaptations
                .findPage(
                    author,
                    status = null,
                    pageRequest,
                ).mapLeft { it.toDomainError() }
                .bind()
                .toDtos()
                .bind()
        }

    /** A plan is visible to its author and to admins. */
    suspend fun get(
        id: AdaptationId,
        caller: UserId,
        callerIsAdmin: Boolean,
    ): Either<DomainError, AdaptationDto> =
        either {
            val item = find(id).bind()
            ensure(callerIsAdmin || item.authorId == caller.value) { DomainError.Forbidden() }
            item.toDto(titleOf(item.innovationId).bind())
        }

    /** The review queue, newest first. */
    suspend fun list(
        status: AdaptationStatus?,
        pageRequest: PageRequest,
    ): Either<DomainError, Page<AdaptationDto>> =
        either {
            adaptations
                .findPage(
                    author = null,
                    status,
                    pageRequest,
                ).mapLeft { it.toDomainError() }
                .bind()
                .toDtos()
                .bind()
        }

    /**
     * Reviews a plan. The decision is one update conditioned on the status that was checked, so two admins cannot both
     * decide; rejecting needs a [comment] for the author to learn why.
     */
    suspend fun review(
        id: AdaptationId,
        next: AdaptationStatus,
        comment: ReviewComment?,
    ): Either<DomainError, AdaptationDto> =
        either {
            ensure(next != AdaptationStatus.REJECTED || comment != null) {
                DomainError.Validation(
                    message = "Komentarz dla autora jest wymagany przy odrzuceniu planu",
                    details =
                        listOf(
                            FieldError("comment", FieldErrorCode.Required, "Komentarz jest wymagany przy odrzuceniu"),
                        ),
                )
            }
            val current = find(id).bind()
            ensure(current.status.canMoveTo(next)) {
                DomainError.Conflict("Plan ma status ${current.status} i nie można go zmienić na $next")
            }
            val reviewed =
                ensureNotNull(
                    adaptations
                        .transition(id, current.status, next, comment, clock.instant().toString())
                        .mapLeft { it.toDomainError() }
                        .bind(),
                ) { DomainError.Conflict("Status planu zmienił się w międzyczasie") }
            reviewed.toDto(titleOf(reviewed.innovationId).bind())
        }

    /** Streams the model's plan, passing on each step that passes the check as soon as it is written. */
    private suspend fun FlowCollector<AdaptationEvent>.consult(
        innovation: InnovationItem,
        institution: Institution,
    ): StreamedAnswer =
        llm.stream(AdaptationPrompts.request(innovation, institution)).fold(StreamedAnswer.START) { answer, piece ->
            val next = answer.after(piece)
            next.finished.forEach { raw ->
                AdaptationParser.step(raw).onRight { emit(AdaptationEvent.Step(it.toDto())) }
            }
            next
        }

    /** Turns what the model said into the closing event, storing the plan if it is one. `null` is a deadline hit. */
    private suspend fun conclude(
        answer: StreamedAnswer?,
        innovation: InnovationItem,
        author: UserId,
        institution: Institution,
    ): AdaptationEvent =
        when {
            answer == null -> {
                done(AiStatus.UNAVAILABLE)
            }

            answer.failure != null -> {
                done(answer.failure.aiStatus())
            }

            else -> {
                AdaptationParser.plan(answer.text, institution.budgetPln).fold(
                    ifLeft = { done(AiStatus.INVALID_OUTPUT) },
                    ifRight = { plan -> store(plan, innovation, author, institution) },
                )
            }
        }

    private suspend fun store(
        plan: AdaptationPlan,
        innovation: InnovationItem,
        author: UserId,
        institution: Institution,
    ): AdaptationEvent {
        val now = clock.instant().toString()
        val item =
            AdaptationItem(
                innovationId = innovation.id,
                authorId = author.value,
                institution = institution,
                plan = plan,
                model = settings.model,
                createdAt = now,
                updatedAt = now,
            )
        return adaptations.create(item).fold(
            ifLeft = { error -> failed(error.toDomainError()) },
            ifRight = { done(AiStatus.OK, it.toDto(innovation.title)) },
        )
    }

    private fun done(
        status: AiStatus,
        adaptation: AdaptationDto? = null,
    ): AdaptationEvent = AdaptationEvent.Done(AdaptationResponse(aiStatus = status, adaptation = adaptation))

    private fun failed(error: DomainError): AdaptationEvent =
        AdaptationEvent.Failed(ErrorResponse(code = error.code, message = error.message))

    private suspend fun find(id: AdaptationId): Either<DomainError, AdaptationItem> =
        either {
            ensureNotNull(adaptations.findById(id).mapLeft { it.toDomainError() }.bind()) {
                DomainError.NotFound("Nie znaleziono planu adaptacji o ID: ${id.value.toHexString()}")
            }
        }

    private suspend fun titleOf(innovationId: ObjectId): Either<DomainError, String> =
        innovations.titlesOf(listOf(innovationId)).mapLeft { it.toDomainError() }.map { it[innovationId].orEmpty() }

    private suspend fun Page<AdaptationItem>.toDtos(): Either<DomainError, Page<AdaptationDto>> =
        either {
            val titles =
                innovations
                    .titlesOf(items.map(AdaptationItem::innovationId).distinct())
                    .mapLeft { it.toDomainError() }
                    .bind()
            Page(
                items = items.map { it.toDto(titles[it.innovationId].orEmpty()) },
                page = page,
                size = size,
                total = total,
            )
        }

    companion object {
        /** Writing a plan takes about twice as long as a suggestion, and it waits for the model like any other. */
        val DEFAULT_DEADLINE: Duration = 40.seconds
    }
}
