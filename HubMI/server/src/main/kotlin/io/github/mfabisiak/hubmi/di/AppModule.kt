package io.github.mfabisiak.hubmi.di

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.repository.ChallengeRepository
import io.github.mfabisiak.hubmi.repository.InnovationRepository
import io.github.mfabisiak.hubmi.repository.MaterialRepository
import io.github.mfabisiak.hubmi.repository.MongoRepository
import io.github.mfabisiak.hubmi.repository.NeedRepository
import io.github.mfabisiak.hubmi.repository.SampleRepository
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import io.github.mfabisiak.hubmi.service.ChallengeService
import io.github.mfabisiak.hubmi.service.GreetingService
import io.github.mfabisiak.hubmi.service.InnovationService
import io.github.mfabisiak.hubmi.service.MatchService
import io.github.mfabisiak.hubmi.service.MaterialService
import io.github.mfabisiak.hubmi.service.SampleService
import io.github.mfabisiak.hubmi.service.matching.InnovationIndex
import io.github.mfabisiak.hubmi.service.matching.KeywordMatchingEngine
import io.github.mfabisiak.hubmi.service.matching.MatchingEngine
import io.github.mfabisiak.hubmi.service.matching.PrefixStemmer
import io.github.mfabisiak.hubmi.service.matching.Stemmer
import io.github.mfabisiak.hubmi.service.matching.TextAnalyzer
import io.github.mfabisiak.hubmi.service.toDomainError
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
        single<Stemmer> { PrefixStemmer() }
        single { TextAnalyzer(get()) }
        single {
            val innovations = get<InnovationRepository>()
            InnovationIndex(get(), load = { innovations.findAllActive().mapLeft { it.toDomainError() } })
        }
        single<MatchingEngine> { KeywordMatchingEngine(get(), get()) }
        single { InnovationService(get(), get()) }
        single { ChallengeRepository(get<MongoRepository>().database) }
        single { ChallengeService(get()) }
        single { MaterialRepository(get<MongoRepository>().database) }
        single { MaterialService(get()) }
        single { NeedRepository(get<MongoRepository>().database) }
        single { MatchService(get(), get()) }
        single { DatabaseSeeder(get<MongoRepository>().database, get()) }
    }
