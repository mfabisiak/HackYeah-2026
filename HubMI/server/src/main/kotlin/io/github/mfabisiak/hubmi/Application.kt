package io.github.mfabisiak.hubmi

import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.di.appModule
import io.github.mfabisiak.hubmi.plugins.MongoIndexes
import io.github.mfabisiak.hubmi.plugins.MongoSchemas
import io.github.mfabisiak.hubmi.plugins.configureKoin
import io.github.mfabisiak.hubmi.plugins.configureSecurity
import io.github.mfabisiak.hubmi.plugins.configureSerialization
import io.github.mfabisiak.hubmi.repository.MongoRepository
import io.github.mfabisiak.hubmi.routes.appRoutes
import io.github.mfabisiak.hubmi.routes.sampleRoutes
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.ktor.ext.get

fun main() {
    embeddedServer(Netty, port = AppConfig.fromEnv().port, host = "0.0.0.0", module = { module() })
        .start(wait = true)
}

fun Application.module(vararg extraModules: Module) {
    configureKoin(appModule, *extraModules)
    configureSerialization()
    configureSecurity()

    val mongo = get<MongoRepository>()
    val seeder = get<DatabaseSeeder>()
    launch {
        MongoIndexes.configure(mongo.database)
        MongoSchemas.configure(mongo.database)
        seeder.seedIfNeeded()
    }

    routing {
        appRoutes()
        sampleRoutes()
    }
}
