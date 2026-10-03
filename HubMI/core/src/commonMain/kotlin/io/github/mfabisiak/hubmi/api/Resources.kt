package io.github.mfabisiak.hubmi.api

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/health")
class Health {
    @Serializable
    @Resource("ready")
    class Ready(
        val parent: Health = Health(),
    )
}

@Serializable
@Resource("/api")
class Api {
    @Serializable
    @Resource("me")
    class Me(
        val parent: Api = Api(),
    )

    @Serializable
    @Resource("admin")
    class Admin(
        val parent: Api = Api(),
    )
}
