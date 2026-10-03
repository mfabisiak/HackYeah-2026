package io.github.mfabisiak.hubmi

import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.di.appModule
import io.github.mfabisiak.hubmi.plugins.configureKoin
import io.github.mfabisiak.hubmi.plugins.configureSecurity
import io.github.mfabisiak.hubmi.plugins.configureSerialization
import io.github.mfabisiak.hubmi.routes.appRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.koin.core.module.Module

fun main() {
    embeddedServer(Netty, port = AppConfig.fromEnv().port, host = "0.0.0.0", module = { module() })
        .start(wait = true)
}

fun Application.module(vararg extraModules: Module) {
    configureKoin(appModule, *extraModules)
    configureSerialization()
    configureSecurity()
    routing { appRoutes() }
}
