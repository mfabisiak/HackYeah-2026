package io.github.mfabisiak.hubmi.tester

import io.github.mfabisiak.hubmi.api.AdminFeedback
import io.github.mfabisiak.hubmi.api.AdminTestRequests
import io.github.mfabisiak.hubmi.api.CreateFeedbackRequest
import io.github.mfabisiak.hubmi.api.CreateTestRequest
import io.github.mfabisiak.hubmi.api.Innovations
import io.github.mfabisiak.hubmi.api.Role
import io.github.mfabisiak.hubmi.api.UpdateTestRequestStatusRequest
import io.github.mfabisiak.hubmi.auth.KEYCLOAK_AUTH
import io.github.mfabisiak.hubmi.auth.UserId
import io.github.mfabisiak.hubmi.auth.currentUser
import io.github.mfabisiak.hubmi.auth.requireRole
import io.github.mfabisiak.hubmi.common.http.respondEither
import io.github.mfabisiak.hubmi.common.orValidationError
import io.github.mfabisiak.hubmi.common.validatePageRequest
import io.github.mfabisiak.hubmi.innovations.InnovationId
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.put
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

private const val INVALID_ID = "Nieprawidłowy identyfikator innowacji"
private const val INVALID_TEST_REQUEST_ID = "Nieprawidłowy identyfikator zgłoszenia do testów"
private const val INVALID_FEEDBACK = "Błąd walidacji oceny"
private const val INVALID_TEST_REQUEST = "Błąd walidacji zgłoszenia do testów"

/**
 * Innovation tester. Any authenticated user may declare willingness to test and rate an innovation (both are `PUT`:
 * a user has one request and one rating per innovation, and repeating the call replaces it); admins review them.
 */
fun Route.testerRoutes() {
    val feedbackService by inject<FeedbackService>()
    val testRequestService by inject<TestRequestService>()

    authenticate(KEYCLOAK_AUTH) {
        put<Innovations.ById.TestRequest> { resource ->
            val request = call.receive<CreateTestRequest>()
            respondEither {
                val user = call.currentUser.bind()
                val innovationId = InnovationId.parse(resource.parent.id).orValidationError(INVALID_ID).bind()
                val draft = TestRequestDraft.parse(request).orValidationError(INVALID_TEST_REQUEST).bind()
                testRequestService.submit(innovationId, UserId(user.id), draft).bind()
            }
        }

        get<Innovations.ById.TestRequest> { resource ->
            respondEither {
                val user = call.currentUser.bind()
                val innovationId = InnovationId.parse(resource.parent.id).orValidationError(INVALID_ID).bind()
                testRequestService.mine(innovationId, UserId(user.id)).bind()
            }
        }

        get<Innovations.ById.Feedback> { resource ->
            respondEither {
                val user = call.currentUser.bind()
                val innovationId = InnovationId.parse(resource.parent.id).orValidationError(INVALID_ID).bind()
                feedbackService.mine(innovationId, UserId(user.id)).bind()
            }
        }

        put<Innovations.ById.Feedback> { resource ->
            val request = call.receive<CreateFeedbackRequest>()
            respondEither {
                val user = call.currentUser.bind()
                val innovationId = InnovationId.parse(resource.parent.id).orValidationError(INVALID_ID).bind()
                val draft = FeedbackDraft.parse(request).orValidationError(INVALID_FEEDBACK).bind()
                feedbackService.submit(innovationId, UserId(user.id), draft).bind()
            }
        }

        requireRole(Role.ADMIN) {
            get<AdminFeedback> { resource ->
                respondEither {
                    val pageRequest = validatePageRequest(resource.page, resource.size).bind()
                    val innovationId =
                        resource.innovationId?.let { InnovationId.parse(it).orValidationError(INVALID_ID).bind() }
                    feedbackService.list(innovationId, pageRequest).bind()
                }
            }

            get<AdminTestRequests> { resource ->
                respondEither {
                    val pageRequest = validatePageRequest(resource.page, resource.size).bind()
                    val innovationId =
                        resource.innovationId?.let { InnovationId.parse(it).orValidationError(INVALID_ID).bind() }
                    testRequestService.list(innovationId, resource.status, pageRequest).bind()
                }
            }

            patch<AdminTestRequests.Status> { resource ->
                val request = call.receive<UpdateTestRequestStatusRequest>()
                respondEither {
                    val id = TestRequestId.parse(resource.id).orValidationError(INVALID_TEST_REQUEST_ID).bind()
                    testRequestService.decide(id, request.status).bind()
                }
            }
        }
    }
}
