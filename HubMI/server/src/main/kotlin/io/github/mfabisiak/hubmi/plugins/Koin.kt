package io.github.mfabisiak.hubmi.plugins

import io.ktor.server.application.*
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin(vararg modules: Module) {
    install(Koin) {
        slf4jLogger()
        modules(*modules)
    }
}
