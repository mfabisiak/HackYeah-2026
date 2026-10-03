package io.github.mfabisiak.hubmi.di

import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.repository.MongoRepository
import io.github.mfabisiak.hubmi.service.GreetingService
import org.koin.dsl.module
import org.koin.dsl.onClose

val appModule =
    module {
        single { AppConfig.fromEnv() }
        single { MongoRepository(get()) } onClose { it?.close() }
        single { GreetingService() }
    }
