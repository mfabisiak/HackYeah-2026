package io.github.mfabisiak.hubmi.health

import io.github.mfabisiak.hubmi.api.Health
import io.github.mfabisiak.hubmi.api.HealthResponse
import io.github.mfabisiak.hubmi.common.mongo.MongoRepository
import io.ktor.http.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.healthRoutes() {
    val greetingService by inject<GreetingService>()
    val mongo by inject<MongoRepository>()

    get("/") {
        call.respondText(greetingService.greet("Ktor"))
    }
    get<Health> {
        call.respond(HealthResponse("UP"))
    }
    get<Health.Ready> {
        mongo.ping().fold(
            ifLeft = { call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse("DOWN")) },
            ifRight = { call.respond(HealthResponse("UP")) },
        )
    }
}
