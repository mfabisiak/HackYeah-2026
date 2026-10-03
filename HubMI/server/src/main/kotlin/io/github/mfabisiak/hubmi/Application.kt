package io.github.mfabisiak.hubmi

import io.github.mfabisiak.hubmi.auth.authRoutes
import io.github.mfabisiak.hubmi.auth.configureSecurity
import io.github.mfabisiak.hubmi.calls.grantCallRoutes
import io.github.mfabisiak.hubmi.challenges.challengeRoutes
import io.github.mfabisiak.hubmi.common.mongo.MongoRepository
import io.github.mfabisiak.hubmi.config.AppConfig
import io.github.mfabisiak.hubmi.contract.contractStubs
import io.github.mfabisiak.hubmi.di.appModule
import io.github.mfabisiak.hubmi.health.healthRoutes
import io.github.mfabisiak.hubmi.ideas.ideaRoutes
import io.github.mfabisiak.hubmi.innovations.innovationRoutes
import io.github.mfabisiak.hubmi.matching.MatchingEngine
import io.github.mfabisiak.hubmi.matching.matchRoutes
import io.github.mfabisiak.hubmi.materials.materialRoutes
import io.github.mfabisiak.hubmi.messaging.NotificationEventListener
import io.github.mfabisiak.hubmi.messaging.messagingRoutes
import io.github.mfabisiak.hubmi.plugins.MongoIndexes
import io.github.mfabisiak.hubmi.plugins.configureKoin
import io.github.mfabisiak.hubmi.plugins.configureSerialization
import io.github.mfabisiak.hubmi.samples.sampleRoutes
import io.github.mfabisiak.hubmi.seeding.DatabaseSeeder
import io.github.mfabisiak.hubmi.tester.testerRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.ktor.ext.get
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
    config: AppConfig,
    vararg extraModules: Module,
) {
    install(CallLogging)
    configureKoin(appModule(config), *extraModules)
    configureSerialization()
    configureSecurity()

    val mongo = get<MongoRepository>()
    val seeder = get<DatabaseSeeder>()
    // Blocking on purpose: the unique indexes are what keeps the upserts safe and the seed fills the catalogue, so
    // neither may race the first request. The server starts accepting connections only after the modules are loaded.
    runBlocking {
        MongoIndexes
            .configure(
                mongo.database,
            ).onLeft { environment.log.error("Creating Mongo indexes failed", it.cause) }
        seeder.seedIfNeeded().onLeft { environment.log.error("Seeding the database failed", it.cause) }
    }

    // Outside the blocking block above: a slow or absent Ollama must not delay start-up, only the first match
    launch { get<MatchingEngine>().warmUp() }

    val notificationEventListener = get<NotificationEventListener>()
    notificationEventListener.start()

    routing {
        healthRoutes()
        authRoutes()
        contractStubs()
        sampleRoutes()
        innovationRoutes()
        challengeRoutes()
        materialRoutes()
        matchRoutes()
        testerRoutes()
        ideaRoutes()
        grantCallRoutes()
        messagingRoutes()
    }
}

fun Application.module(vararg extraModules: Module) = module(AppConfig(), *extraModules)
