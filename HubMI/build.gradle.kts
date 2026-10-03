plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.ktlint) apply false
}

// Style checks apply to the backend modules; Compose/Android modules are not linted yet.
subprojects {
    if (path == ":server" || path == ":core") {
        apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(rootProject.libs.versions.ktlint.get())
        }
    }
}
