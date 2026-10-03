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
import kotlin.system.exitProcess

fun main() {
    AppConfig.fromEnv().fold(
        ifLeft = {
            System.err.println("Invalid configuration: $it")
            exitProcess(1)
        },
        ifRight = { config ->
            embeddedServer(Netty, port = config.port, host = "0.0.0.0") { module(config) }
                .start(wait = true)
        },
    )
}

fun Application.module(
    config: AppConfig = AppConfig(),
    vararg extraModules: Module,
) {
    configureKoin(appModule(config), *extraModules)
    configureSerialization()
    configureSecurity()
    routing { appRoutes() }
}
