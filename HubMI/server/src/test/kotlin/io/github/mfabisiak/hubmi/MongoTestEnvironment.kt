package io.github.mfabisiak.hubmi

import org.testcontainers.containers.MongoDBContainer

object MongoTestEnvironment {
    private var container: MongoDBContainer? = null

    val connectionString: String by lazy {
        try {
            val c = MongoDBContainer("mongo:8")
            c.start()
            container = c
            c.connectionString
        } catch (_: Throwable) {
            System.getenv("MONGO_URI") ?: "mongodb://localhost:27017/?directConnection=true"
        }
    }

    fun stop() {
        container?.stop()
        container = null
    }
}
