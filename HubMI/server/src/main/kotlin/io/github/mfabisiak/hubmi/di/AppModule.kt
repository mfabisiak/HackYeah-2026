package io.github.mfabisiak.hubmi.di

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.github.mfabisiak.hubmi.admin.AdminDashboardService
import io.github.mfabisiak.hubmi.calls.ApplicationRepository
import io.github.mfabisiak.hubmi.calls.ApplicationService
import io.github.mfabisiak.hubmi.calls.GrantCallRepository
import io.github.mfabisiak.hubmi.calls.GrantCallService
import io.github.mfabisiak.hubmi.challenges.ChallengeRepository
import io.github.mfabisiak.hubmi.challenges.ChallengeService
import io.github.mfabisiak.hubmi.common.mongo.MongoRepository
import io.github.mfabisiak.hubmi.common.toDomainError
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.config.MatchingMode
import io.github.mfabisiak.hubmi.health.GreetingService
import io.github.mfabisiak.hubmi.ideas.EventPublisher
import io.github.mfabisiak.hubmi.ideas.IdeaRepository
import io.github.mfabisiak.hubmi.ideas.IdeaService
import io.github.mfabisiak.hubmi.innovations.InnovationRepository
import io.github.mfabisiak.hubmi.innovations.InnovationService
import io.github.mfabisiak.hubmi.matching.EmbeddingClient
import io.github.mfabisiak.hubmi.matching.FallbackEngine
import io.github.mfabisiak.hubmi.matching.HybridMatchingEngine
import io.github.mfabisiak.hubmi.matching.InnovationIndex
import io.github.mfabisiak.hubmi.matching.KeywordMatchingEngine
import io.github.mfabisiak.hubmi.matching.MatchService
import io.github.mfabisiak.hubmi.matching.MatchingEngine
import io.github.mfabisiak.hubmi.matching.NeedRepository
import io.github.mfabisiak.hubmi.matching.OllamaEmbeddingClient
import io.github.mfabisiak.hubmi.matching.PrefixStemmer
import io.github.mfabisiak.hubmi.matching.Stemmer
import io.github.mfabisiak.hubmi.matching.TextAnalyzer
import io.github.mfabisiak.hubmi.matching.VectorIndex
import io.github.mfabisiak.hubmi.materials.MaterialRepository
import io.github.mfabisiak.hubmi.materials.MaterialService
import io.github.mfabisiak.hubmi.messaging.CoroutineEventBus
import io.github.mfabisiak.hubmi.messaging.EventBus
import io.github.mfabisiak.hubmi.messaging.MessageRepository
import io.github.mfabisiak.hubmi.messaging.NotificationEventListener
import io.github.mfabisiak.hubmi.messaging.NotificationRepository
import io.github.mfabisiak.hubmi.messaging.NotificationService
import io.github.mfabisiak.hubmi.messaging.ThreadRepository
import io.github.mfabisiak.hubmi.messaging.ThreadService
import io.github.mfabisiak.hubmi.samples.SampleRepository
import io.github.mfabisiak.hubmi.samples.SampleService
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import io.github.mfabisiak.hubmi.tester.FeedbackRepository
import io.github.mfabisiak.hubmi.tester.FeedbackService
import io.github.mfabisiak.hubmi.tester.TestRequestRepository
import io.github.mfabisiak.hubmi.tester.TestRequestService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.net.URI
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val OLLAMA_CONNECT_TIMEOUT_MILLIS = 2_000L
private const val OLLAMA_REQUEST_TIMEOUT_MILLIS = 30_000L

fun appModule(config: AppConfig) =
    module {
        single { config }
        single { MongoRepository(get()) } onClose { it?.close() }
        single<JwkProvider> {
            JwkProviderBuilder(URI(config.keycloakJwksUrl).toURL())
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()
        }
        single { GreetingService() }
        single<Clock> { Clock.systemUTC() }
        single<EventBus> { CoroutineEventBus() }
        single<EventPublisher> { get<EventBus>() }
        single { NotificationEventListener(get(), get()) }
        single { SampleRepository(get<MongoRepository>().database) }
        single { InnovationRepository(get<MongoRepository>().database) }
        single<Stemmer> { PrefixStemmer() }
        single { TextAnalyzer(get()) }
        single {
            val innovations = get<InnovationRepository>()
            InnovationIndex(get(), load = { innovations.findAllActive().mapLeft { it.toDomainError() } })
        }
        single { KeywordMatchingEngine(get(), get()) }
        single {
            HttpClient(CIO) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(HttpTimeout) {
                    connectTimeoutMillis = OLLAMA_CONNECT_TIMEOUT_MILLIS
                    requestTimeoutMillis = OLLAMA_REQUEST_TIMEOUT_MILLIS
                }
            }
        } onClose { it?.close() }
        single<EmbeddingClient> {
            val settings = get<AppConfig>()
            OllamaEmbeddingClient(get(), settings.ollamaUrl, settings.embeddingModel)
        }
        single<MatchingEngine> {
            val keyword = get<KeywordMatchingEngine>()
            when (get<AppConfig>().matchingMode) {
                MatchingMode.KEYWORD -> {
                    keyword
                }

                MatchingMode.HYBRID -> {
                    FallbackEngine(
                        primary = HybridMatchingEngine(get(), VectorIndex(get()), get(), keyword),
                        fallback = keyword,
                    )
                }
            }
        }
        single { ChallengeRepository(get<MongoRepository>().database) }
        single { MaterialRepository(get<MongoRepository>().database) }
        single { IdeaRepository(get<MongoRepository>().database) }
        single { GrantCallRepository(get<MongoRepository>().database) }
        single { ApplicationRepository(get<MongoRepository>().database) }
        single { NeedRepository(get<MongoRepository>().database) }
        single { ThreadRepository(get<MongoRepository>().database) }
        single { MessageRepository(get<MongoRepository>().database) }
        single { NotificationRepository(get<MongoRepository>().database) }
        single { SampleService(get()) }
        single { InnovationService(get(), get()) }
        single { ChallengeService(get()) }
        single { MaterialService(get()) }
        single { IdeaService(get(), get(), get()) }
        single { GrantCallService(get(), get(), get(), get()) }
        single { ApplicationService(get(), get(), get(), get()) }
        single { MatchService(get(), get()) }
        single { FeedbackRepository(get<MongoRepository>().database) }
        single { FeedbackService(get(), get()) }
        single { TestRequestRepository(get<MongoRepository>().database) }
        single { TestRequestService(get(), get()) }
        single { ThreadService(get(), get(), get(), get()) }
        single { NotificationService(get()) }
        single { AdminDashboardService(get(), get(), get(), get(), get(), get()) }
        single { DatabaseSeeder(get<MongoRepository>().database, get()) }
    }
