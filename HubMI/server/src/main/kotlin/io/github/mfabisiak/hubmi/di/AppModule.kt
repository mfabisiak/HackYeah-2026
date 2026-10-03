package io.github.mfabisiak.hubmi.di

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.repository.ApplicationRepository
import io.github.mfabisiak.hubmi.repository.ChallengeRepository
import io.github.mfabisiak.hubmi.repository.GrantCallRepository
import io.github.mfabisiak.hubmi.repository.IdeaRepository
import io.github.mfabisiak.hubmi.repository.InnovationRepository
import io.github.mfabisiak.hubmi.repository.MaterialRepository
import io.github.mfabisiak.hubmi.repository.MongoRepository
import io.github.mfabisiak.hubmi.repository.SampleRepository
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import io.github.mfabisiak.hubmi.service.ChallengeService
import io.github.mfabisiak.hubmi.service.EventPublisher
import io.github.mfabisiak.hubmi.service.GrantCallService
import io.github.mfabisiak.hubmi.service.GreetingService
import io.github.mfabisiak.hubmi.service.IdeaService
import io.github.mfabisiak.hubmi.service.InnovationService
import io.github.mfabisiak.hubmi.service.MaterialService
import io.github.mfabisiak.hubmi.service.NoOpEventPublisher
import io.github.mfabisiak.hubmi.service.SampleService
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.net.URI
import java.time.Clock
import java.util.concurrent.TimeUnit

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
        single<EventPublisher> { NoOpEventPublisher() }
        single { SampleRepository(get<MongoRepository>().database) }
        single { InnovationRepository(get<MongoRepository>().database) }
        single { ChallengeRepository(get<MongoRepository>().database) }
        single { MaterialRepository(get<MongoRepository>().database) }
        single { IdeaRepository(get<MongoRepository>().database) }
        single { GrantCallRepository(get<MongoRepository>().database) }
        single { ApplicationRepository(get<MongoRepository>().database) }
        single { SampleService(get()) }
        single { InnovationService(get()) }
        single { ChallengeService(get()) }
        single { MaterialService(get()) }
        single { IdeaService(get(), get()) }
        single { GrantCallService(get(), get(), get(), get()) }
        single { DatabaseSeeder(get<MongoRepository>().database, get()) }
    }
