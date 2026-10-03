package io.github.mfabisiak.hubmi

import org.testcontainers.containers.MongoDBContainer

object MongoTestEnvironment {
    val connectionString: String by lazy {
        try {
            MongoDBContainer("mongo:8").apply { start() }.connectionString
        } catch (_: Throwable) {
            System.getenv("MONGO_URI") ?: "mongodb://localhost:27017/?directConnection=true"
        }
    }
}
