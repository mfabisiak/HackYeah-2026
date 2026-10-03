package io.github.mfabisiak.hubmi.admin

import arrow.core.toEitherNel
import io.github.mfabisiak.hubmi.api.AdminSummary
import io.github.mfabisiak.hubmi.api.AdminTrends
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.requireRole
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.common.orValidationError
import io.ktor.server.auth.*
import io.ktor.server.resources.get
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.adminRoutes() {
    val dashboardService by inject<AdminDashboardService>()

    authenticate(KEYCLOAK_AUTH) {
        requireRole(Role.ADMIN) {
            get<AdminTrends> { params ->
                respondEither {
                    val months =
                        TrendsMonths
                            .parse(params.months)
                            .toEitherNel()
                            .orValidationError("Nieprawidłowe parametry zapytania")
                            .bind()
                    dashboardService.getTrends(months).bind()
                }
            }

            get<AdminSummary> {
                respondEither {
                    dashboardService.getSummary().bind()
                }
            }
        }
    }
}
