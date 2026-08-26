package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationSessionAdminRevokeRequest
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AuthenticationSessionAdminControllerTest {

    private val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
    private val userAccountService = mock(IUserAccountService::class.java)
    private val controller = AuthenticationSessionAdminController(sessionService, userAccountService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "t-1", "administrator")
            }
        )
        `when`(userAccountService.getUserRecord("u-1"))
            .thenReturn(UserAccountRow(id = "u-1", tenantId = "t-1"))
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun list_returnsOnlyActiveSessionsForTargetUser() {
        val active = issue()
        val revoked = issue()
        issue(userId = "u-other")
        sessionService.revokeForUser(revoked.id, "t-1", "u-1", "USER_REVOKE")

        val result = controller.list("u-1")

        assertEquals(listOf(active.id), result.map(AuthenticationSession::id))
    }

    @Test
    fun revoke_marksOnlyTheRequestedOwnedSession() {
        val target = issue()
        val other = issue()

        assertTrue(
            controller.revoke(
                target.id,
                AuthenticationSessionAdminRevokeRequest("u-1", "suspected compromise"),
            )
        )

        val revoked = assertNotNull(sessionService.get(target.id))
        assertFalse(revoked.isActive())
        assertEquals("ADMIN_REVOKE:suspected compromise", revoked.revokeReason)
        assertTrue(assertNotNull(sessionService.get(other.id)).isActive())
    }

    @Test
    fun revoke_rejectsSessionOwnedByAnotherUser() {
        val other = issue(userId = "u-other")

        assertFalse(
            controller.revoke(
                other.id,
                AuthenticationSessionAdminRevokeRequest("u-1", "security review"),
            )
        )
        assertTrue(assertNotNull(sessionService.get(other.id)).isActive())
    }

    @Test
    fun list_hidesCrossTenantAndMissingUsersBehindTheSameResponse() {
        `when`(userAccountService.getUserRecord("cross-tenant"))
            .thenReturn(UserAccountRow(id = "cross-tenant", tenantId = "t-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.list("cross-tenant") }
        val missing = assertFailsWith<ResponseStatusException> { controller.list("missing") }

        assertEquals(404, crossTenant.statusCode.value())
        assertEquals(crossTenant.reason, missing.reason)
    }

    @Test
    fun list_requiresAuthenticatedAdministrator() {
        KudosContextHolder.clear()

        val error = assertFailsWith<ResponseStatusException> { controller.list("u-1") }

        assertEquals(401, error.statusCode.value())
    }

    @Test
    fun revoke_requiresAuditableReason() {
        val issued = issue()

        val error = assertFailsWith<ResponseStatusException> {
            controller.revoke(issued.id, AuthenticationSessionAdminRevokeRequest("u-1", " "))
        }

        assertEquals(400, error.statusCode.value())
        assertTrue(assertNotNull(sessionService.get(issued.id)).isActive())
    }

    @Test
    fun endpointsDeclareConcretePermissions() {
        val list = AuthenticationSessionAdminController::class.java
            .getDeclaredMethod("list", String::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val revoke = AuthenticationSessionAdminController::class.java
            .getDeclaredMethod(
                "revoke",
                String::class.java,
                AuthenticationSessionAdminRevokeRequest::class.java,
            )
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:session:view", list.value)
        assertEquals("auth:session:revoke", revoke.value)
    }

    private fun issue(userId: String = "u-1") = sessionService.issue(
        AuthenticationSessionIssueCommand(
            context = AuthenticationContext(
                userId = userId,
                tenantId = "t-1",
                authTime = Instant.now(),
                amr = setOf("password"),
                acr = "urn:kudos:acr:password",
            ),
            username = "alice",
            loginDevice = "PC",
            loginBrowser = "Chrome",
        )
    )
}
