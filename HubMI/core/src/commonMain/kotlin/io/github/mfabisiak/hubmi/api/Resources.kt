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

/** Root of the REST API (`/api`). Module resources hang off it, see the other files in this package. */
@Serializable
@Resource("/api")
class Api {
    @Serializable
    @Resource("me")
    class Me(
        val parent: Api = Api(),
    )

    /** Administrator area (`/api/admin`); sub-resources: [AdminTrends], [AdminIdeas]. */
    @Serializable
    @Resource("admin")
    class Admin(
        val parent: Api = Api(),
    )
}
