plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()

    js {
        browser()
        compilerOptions {
            target = "es2015"
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
