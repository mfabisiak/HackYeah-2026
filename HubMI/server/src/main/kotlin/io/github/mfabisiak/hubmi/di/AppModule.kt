package io.github.mfabisiak.hubmi.di

import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.service.GreetingService
import org.koin.dsl.module

val appModule = module {
    single { AppConfig.fromEnv() }
    single { GreetingService() }
}
