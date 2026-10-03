plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "io.github.mfabisiak.hubmi"
version = "1.0.0"
application {
    mainClass = "io.github.mfabisiak.hubmi.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverResources)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    implementation(libs.arrow.core)
    implementation(libs.mongodb.driverKotlinCoroutine)
    implementation(libs.mongodb.driverKotlinExtensions)
    implementation(libs.bson.kotlinx)
    testImplementation(libs.lucene.analysisStempel)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientMock)
    testImplementation(libs.ktor.clientResources)
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.testcontainers.mongodb)
}
