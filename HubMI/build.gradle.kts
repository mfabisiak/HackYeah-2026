plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint.get())
    }
}

// Everything CI needs for the backend; the web frontend is checked by its own job.
tasks.register("backendCheck") {
    group = "verification"
    description = "Runs ktlint and tests for :server and :core (JVM)."
    dependsOn(":server:check", ":core:ktlintCheck", ":core:jvmTest")
}
