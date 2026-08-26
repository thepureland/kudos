package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventNotificationRouteAdminSaveRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAppliesToEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteFallbackEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthSecurityEventNotificationRouteAdminControllerTest {
    private val service = mock(IAuthSecurityEventNotificationRouteConfigService::class.java)
    private val userAccountService = mock(IUserAccountService::class.java)
    private val controller = AuthSecurityEventNotificationRouteAdminController(service, userAccountService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun saveAndListPinTheTenantAndTheOperatorToTheAdministratorSession() {
        `when`(service.listByTenant("tenant-1")).thenReturn(listOf(config()))
        `when`(userAccountService.getUserRecord("duty-1")).thenReturn(record("tenant-1"))
        `when`(service.save(anySaveCommand())).thenReturn(config())

        val listed = controller.list().single()
        val saved = controller.save(request())

        assertEquals("ASSIGNEE_EMAIL", listed.routeCode)
        assertEquals(setOf("EMAIL"), listed.channels)
        assertEquals(2L, saved.configVersion)
        val command = ArgumentCaptor.forClass(AuthSecurityEventNotificationRouteSaveCommand::class.java)
        verify(service).save(command.capture() ?: anySaveCommand())
        assertEquals("tenant-1", command.value.tenantId)
        assertEquals("admin-1", command.value.actorUserId)
        assertEquals(setOf("duty-1"), command.value.responderUserIds)
        assertEquals(1L, command.value.expectedVersion)
    }

    @Test
    fun respondersOutsideTheAdministratorsOwnTenantAreReportedAsAbsent() {
        `when`(userAccountService.getUserRecord("duty-1")).thenReturn(record("tenant-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.save(request()) }

        assertEquals(404, crossTenant.statusCode.value())
        verifyNoInteractions(service)
    }

    @Test
    fun endpointsUseDedicatedPermissionsAndRequireASession() {
        val listPermission = AuthSecurityEventNotificationRouteAdminController::class.java
            .getDeclaredMethod("list")
            .getAnnotation(RequiresPermission::class.java)
        val savePermission = AuthSecurityEventNotificationRouteAdminController::class.java
            .getDeclaredMethod("save", AuthSecurityEventNotificationRouteAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        KudosContextHolder.clear()

        val unauthorized = assertFailsWith<ResponseStatusException> { controller.list() }

        assertEquals(401, unauthorized.statusCode.value())
        assertEquals("auth:security-event-notification-route:view", listPermission.value)
        assertEquals("auth:security-event-notification-route:update", savePermission.value)
        verifyNoInteractions(service)
    }

    @Test
    fun staleVersionsConflictWhileMalformedRulesAreRejected() {
        `when`(userAccountService.getUserRecord("duty-1")).thenReturn(record("tenant-1"))
        `when`(service.save(anySaveCommand())).thenThrow(
            AuthSecurityEventException("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT"),
            AuthSecurityEventException("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_CODE_INVALID"),
        )

        val conflict = assertFailsWith<ResponseStatusException> { controller.save(request()) }
        val invalid = assertFailsWith<ResponseStatusException> { controller.save(request()) }
        val blankResponder = assertFailsWith<ResponseStatusException> {
            controller.save(request(responderUserIds = setOf(" ")))
        }

        assertEquals(409, conflict.statusCode.value())
        assertEquals(400, invalid.statusCode.value())
        assertEquals(400, blankResponder.statusCode.value())
    }

    private fun request(responderUserIds: Set<String> = setOf(" duty-1 ")) =
        AuthSecurityEventNotificationRouteAdminSaveRequest(
            notificationType = "SLA_ESCALATED",
            appliesTo = "ASSIGNED",
            routeCode = "ASSIGNEE_EMAIL",
            destination = "USER",
            channels = setOf("EMAIL"),
            responderUserIds = responderUserIds,
            includeAssignee = true,
            enabled = true,
            fallbackBehavior = "DEFAULT_ROUTE",
            expectedVersion = 1L,
            reason = "duty roster change",
        )

    private fun anySaveCommand() = AuthSecurityEventNotificationRouteSaveCommand(
        tenantId = "tenant-1",
        notificationType = "SLA_ESCALATED",
        appliesTo = "ASSIGNED",
        routeCode = "ASSIGNEE_EMAIL",
        destination = "USER",
        channels = setOf("EMAIL"),
        responderUserIds = setOf("duty-1"),
        responderRosterCode = null,
        includeAssignee = true,
        enabled = true,
        fallbackBehavior = "DEFAULT_ROUTE",
        expectedVersion = 1L,
        actorUserId = "admin-1",
        reason = "duty roster change",
    )

    private fun config() = AuthSecurityEventNotificationRouteConfig(
        tenantId = "tenant-1",
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        appliesTo = AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED,
        routeCode = "ASSIGNEE_EMAIL",
        destination = AuthSecurityEventNotificationDestinationEnum.USER,
        channels = setOf(AuthSecurityEventNotificationChannelEnum.EMAIL),
        responderUserIds = setOf("duty-1"),
        responderRosterCode = null,
        includeAssignee = true,
        enabled = true,
        fallbackBehavior = AuthSecurityEventNotificationRouteFallbackEnum.DEFAULT_ROUTE,
        configVersion = 2L,
        createUserId = "admin-1",
        createReason = "initial",
        createTime = NOW,
        updateUserId = "admin-1",
        updateReason = "duty roster change",
        updateTime = NOW,
    )

    private fun record(tenantId: String) = UserAccountRow(id = "duty-1", tenantId = tenantId)

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.parse("2026-08-25T10:00:00")
    }
}
