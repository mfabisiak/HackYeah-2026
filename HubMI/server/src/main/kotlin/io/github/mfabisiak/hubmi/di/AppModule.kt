package io.github.mfabisiak.hubmi.di

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.github.mfabisiak.hubmi.challenges.ChallengeRepository
import io.github.mfabisiak.hubmi.challenges.ChallengeService
import io.github.mfabisiak.hubmi.common.mongo.MongoRepository
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.health.GreetingService
import io.github.mfabisiak.hubmi.innovations.InnovationRepository
import io.github.mfabisiak.hubmi.innovations.InnovationService
import io.github.mfabisiak.hubmi.materials.MaterialRepository
import io.github.mfabisiak.hubmi.materials.MaterialService
import io.github.mfabisiak.hubmi.samples.SampleRepository
import io.github.mfabisiak.hubmi.samples.SampleService
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import org.koin.dsl.module
import org.koin.dsl.onClose
import java.net.URI
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
        single { SampleRepository(get<MongoRepository>().database) }
        single { SampleService(get()) }
        single { InnovationRepository(get<MongoRepository>().database) }
        single { InnovationService(get()) }
        single { ChallengeRepository(get<MongoRepository>().database) }
        single { ChallengeService(get()) }
        single { MaterialRepository(get<MongoRepository>().database) }
        single { MaterialService(get()) }
        single { DatabaseSeeder(get<MongoRepository>().database, get()) }
    }
