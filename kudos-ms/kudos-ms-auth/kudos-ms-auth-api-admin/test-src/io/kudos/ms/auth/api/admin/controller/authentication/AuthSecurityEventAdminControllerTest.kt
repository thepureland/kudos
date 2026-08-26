package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventAcknowledgeRequest
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventAssignRequest
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventCloseRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAcknowledgeCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAssignCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventCloseCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventSummary
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthSecurityEventAdminControllerTest {
    private val events = mock(IAuthSecurityEventService::class.java)
    private val accounts = mock(IUserAccountService::class.java)
    private val controller = AuthSecurityEventAdminController(events, accounts)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
        `when`(accounts.getUserRecord("user-1"))
            .thenReturn(UserAccountRow(id = "user-1", tenantId = "tenant-1"))
        `when`(accounts.getUserRecord("admin-2"))
            .thenReturn(UserAccountRow(id = "admin-2", tenantId = "tenant-1"))
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun listPinsTenantAndValidatesOptionalTargetUser() {
        val summary = mock(AuthSecurityEventSummary::class.java)
        `when`(events.listRecent("tenant-1", "user-1", "CRITICAL", "OPEN", "admin-2", true, 50))
            .thenReturn(listOf(summary))

        val result = controller.list(" user-1 ", " CRITICAL ", " OPEN ", " admin-2 ", true, 50)

        assertEquals(listOf(summary), result)
        verify(accounts).getUserRecord("user-1")
        verify(accounts).getUserRecord("admin-2")
        verify(events).listRecent("tenant-1", "user-1", "CRITICAL", "OPEN", "admin-2", true, 50)
    }

    @Test
    fun listWithoutUserQueriesOnlyCurrentTenant() {
        controller.list(limit = 100)

        verify(events).listRecent("tenant-1", null, null, null, null, false, 100)
        verifyNoInteractions(accounts)
    }

    @Test
    fun crossTenantAndMissingTargetsUseTheSame404() {
        `when`(accounts.getUserRecord("cross"))
            .thenReturn(UserAccountRow(id = "cross", tenantId = "tenant-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.list(userId = "cross") }
        val missing = assertFailsWith<ResponseStatusException> { controller.list(userId = "missing") }

        assertEquals(404, crossTenant.statusCode.value())
        assertEquals(crossTenant.reason, missing.reason)
        verifyNoInteractions(events)
    }

    @Test
    fun assignmentRejectsMissingAndCrossTenantAssigneesWithTheSame404() {
        `when`(accounts.getUserRecord("cross"))
            .thenReturn(UserAccountRow(id = "cross", tenantId = "tenant-2"))
        val request = AuthSecurityEventAssignRequest("cross", 0, "primary responder")

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.assign("event-1", request) }
        val missing = assertFailsWith<ResponseStatusException> {
            controller.assign("event-1", request.copy(assigneeUserId = "missing"))
        }

        assertEquals(404, crossTenant.statusCode.value())
        assertEquals(crossTenant.reason, missing.reason)
        verifyNoInteractions(events)
    }

    @Test
    fun invalidFilterAndLimitReturnBadRequest() {
        val invalidLimit = assertFailsWith<ResponseStatusException> { controller.list(limit = 501) }
        `when`(events.listRecent("tenant-1", null, "UNKNOWN", null, null, false, 100))
            .thenThrow(AuthSecurityEventException("AUTH_SECURITY_EVENT_RISK_LEVEL_INVALID"))
        val invalidRisk = assertFailsWith<ResponseStatusException> { controller.list(riskLevel = "UNKNOWN") }
        `when`(events.listRecent("tenant-1", null, null, "UNKNOWN", null, false, 100))
            .thenThrow(AuthSecurityEventException("AUTH_SECURITY_EVENT_STATUS_INVALID"))
        val invalidStatus = assertFailsWith<ResponseStatusException> { controller.list(status = "UNKNOWN") }

        assertEquals(400, invalidLimit.statusCode.value())
        assertEquals(400, invalidRisk.statusCode.value())
        assertEquals(400, invalidStatus.statusCode.value())
    }

    @Test
    fun workflowCommandsPinTenantAndActorFromCurrentSession() {
        val summary = mock(AuthSecurityEventSummary::class.java)
        `when`(events.acknowledge(any(AuthSecurityEventAcknowledgeCommand::class.java) ?: acknowledgeCommand()))
            .thenReturn(summary)
        `when`(events.close(any(AuthSecurityEventCloseCommand::class.java) ?: closeCommand()))
            .thenReturn(summary)
        `when`(events.assign(any(AuthSecurityEventAssignCommand::class.java) ?: assignCommand()))
            .thenReturn(summary)

        assertEquals(
            summary,
            controller.acknowledge("event-1", AuthSecurityEventAcknowledgeRequest(3, "investigating")),
        )
        assertEquals(
            summary,
            controller.close("event-1", AuthSecurityEventCloseRequest(4, "MITIGATED", "credential replaced")),
        )
        assertEquals(
            summary,
            controller.assign("event-1", AuthSecurityEventAssignRequest(" admin-2 ", 5, "primary responder")),
        )

        val acknowledge = ArgumentCaptor.forClass(AuthSecurityEventAcknowledgeCommand::class.java)
        val close = ArgumentCaptor.forClass(AuthSecurityEventCloseCommand::class.java)
        val assign = ArgumentCaptor.forClass(AuthSecurityEventAssignCommand::class.java)
        verify(events).acknowledge(acknowledge.capture() ?: acknowledgeCommand())
        verify(events).close(close.capture() ?: closeCommand())
        verify(events).assign(assign.capture() ?: assignCommand())
        assertEquals("tenant-1", acknowledge.value.tenantId)
        assertEquals("admin-1", acknowledge.value.actorUserId)
        assertEquals(3, acknowledge.value.expectedVersion)
        assertEquals("tenant-1", close.value.tenantId)
        assertEquals("admin-1", close.value.actorUserId)
        assertEquals("MITIGATED", close.value.resolution)
        assertEquals("tenant-1", assign.value.tenantId)
        assertEquals("admin-1", assign.value.actorUserId)
        assertEquals("admin-2", assign.value.assigneeUserId)
        assertEquals(5, assign.value.expectedVersion)
    }

    @Test
    fun workflowErrorsUseNotFoundConflictAndBadRequestSemantics() {
        `when`(events.acknowledge(any(AuthSecurityEventAcknowledgeCommand::class.java) ?: acknowledgeCommand()))
            .thenThrow(
                AuthSecurityEventException("AUTH_SECURITY_EVENT_NOT_FOUND"),
                AuthSecurityEventException("AUTH_SECURITY_EVENT_VERSION_CONFLICT"),
                AuthSecurityEventException("AUTH_SECURITY_EVENT_REASON_INVALID"),
            )

        val request = AuthSecurityEventAcknowledgeRequest(0, "investigating")
        val missing = assertFailsWith<ResponseStatusException> { controller.acknowledge("event-1", request) }
        val conflict = assertFailsWith<ResponseStatusException> { controller.acknowledge("event-1", request) }
        val invalid = assertFailsWith<ResponseStatusException> { controller.acknowledge("event-1", request) }

        assertEquals(404, missing.statusCode.value())
        assertEquals(409, conflict.statusCode.value())
        assertEquals(400, invalid.statusCode.value())
    }

    @Test
    fun endpointRequiresSessionAndDedicatedPermission() {
        KudosContextHolder.clear()
        val unauthorized = assertFailsWith<ResponseStatusException> { controller.list() }
        val permission = AuthSecurityEventAdminController::class.java
            .getDeclaredMethod(
                "list",
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            .getAnnotation(RequiresPermission::class.java)
        val acknowledgePermission = AuthSecurityEventAdminController::class.java
            .getDeclaredMethod(
                "acknowledge",
                String::class.java,
                AuthSecurityEventAcknowledgeRequest::class.java,
            ).getAnnotation(RequiresPermission::class.java)
        val closePermission = AuthSecurityEventAdminController::class.java
            .getDeclaredMethod("close", String::class.java, AuthSecurityEventCloseRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val assignPermission = AuthSecurityEventAdminController::class.java
            .getDeclaredMethod("assign", String::class.java, AuthSecurityEventAssignRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals(401, unauthorized.statusCode.value())
        assertEquals("auth:security-event:view", permission.value)
        assertEquals("auth:security-event:acknowledge", acknowledgePermission.value)
        assertEquals("auth:security-event:close", closePermission.value)
        assertEquals("auth:security-event:assign", assignPermission.value)
        verifyNoInteractions(events, accounts)
    }

    private fun acknowledgeCommand() = AuthSecurityEventAcknowledgeCommand(
        tenantId = "tenant-1",
        eventId = "event-1",
        actorUserId = "admin-1",
        reason = "investigating",
        expectedVersion = 0,
    )

    private fun closeCommand() = AuthSecurityEventCloseCommand(
        tenantId = "tenant-1",
        eventId = "event-1",
        actorUserId = "admin-1",
        resolution = "MITIGATED",
        reason = "credential replaced",
        expectedVersion = 1,
    )

    private fun assignCommand() = AuthSecurityEventAssignCommand(
        tenantId = "tenant-1",
        eventId = "event-1",
        actorUserId = "admin-1",
        assigneeUserId = "admin-2",
        reason = "primary responder",
        expectedVersion = 0,
    )
}
