plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

// Kotlin/JS library consumed by the React app in /web (npm: "hubmi-client", TypeScript definitions generated).
kotlin {
    js {
        outputModuleName = "hubmi-client"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }

    sourceSets {
        jsMain.dependencies {
            api(project(":core"))
            implementation(libs.arrow.core)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientJs)
            implementation(libs.ktor.clientResources)
            implementation(libs.ktor.clientContentNegotiationCommon)
            implementation(libs.ktor.serializationKotlinxJsonCommon)
        }
    }
}
