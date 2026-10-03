package io.github.mfabisiak.hubmi

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform