package io.github.mfabisiak.hubmi

import io.github.mfabisiak.hubmi.config.AppConfig

/**
 * For tests that never touch Mongo: the server initialises the database at startup, and a short selection timeout
 * keeps that blocking step from waiting 30 seconds when no database is running.
 */
val offlineConfig =
    AppConfig(mongoUri = "mongodb://localhost:27017/?directConnection=true&serverSelectionTimeoutMS=300")
