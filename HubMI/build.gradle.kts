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

// Local dev loop (IntelliJ run configurations in .run/): infrastructure in Docker, server and web outside of it.
// Plain Gradle tasks instead of shell scripts, so the same setup works on Windows, macOS and Linux.
val isWindows = System.getProperty("os.name").startsWith("Windows")
val webDir = layout.projectDirectory.dir("web")

fun npm(vararg args: String) = (if (isWindows) listOf("cmd", "/c", "npm") else listOf("npm")) + args

tasks.register<Exec>("devStopApp") {
    group = "dev"
    description = "Stops the dockerized server and web, which would take over ports 8080 and 3000."
    commandLine("docker", "compose", "stop", "server", "web")
    isIgnoreExitValue = true
}

tasks.register<Exec>("devInfra") {
    group = "dev"
    description = "Starts Postgres, Mongo and Keycloak in Docker and waits until they are healthy."
    dependsOn("devStopApp")
    commandLine("docker", "compose", "up", "-d", "--wait", "postgres", "mongo", "keycloak")
}

tasks.register<Exec>("webInstall") {
    group = "dev"
    description = "Runs npm ci in web/ when package.json or package-lock.json changed."
    // npm resolves the "hubmi-client" dependency from the Kotlin/JS build output
    dependsOn(":web-client:jsBrowserProductionLibraryDistribution")
    workingDir(webDir)
    commandLine(npm("ci"))
    inputs.files(webDir.file("package.json"), webDir.file("package-lock.json"))
    outputs.file(webDir.file("node_modules/.package-lock.json"))
}

tasks.register<Exec>("webDev") {
    group = "dev"
    description = "Runs the Vite dev server (:5173, proxies to the server on :8080)."
    dependsOn("webInstall")
    workingDir(webDir)
    commandLine(npm("run", "dev"))
}

// Everything CI needs for the backend; the web frontend is checked by its own job.
tasks.register("backendCheck") {
    group = "verification"
    description = "Runs ktlint and tests for :server and :core (JVM)."
    dependsOn(":server:check", ":core:ktlintCheck", ":core:jvmTest")
}
