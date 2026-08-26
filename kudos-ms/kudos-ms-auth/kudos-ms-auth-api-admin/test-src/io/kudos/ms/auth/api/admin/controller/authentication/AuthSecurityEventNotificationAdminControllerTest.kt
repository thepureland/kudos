package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventNotificationReplayRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationSummary
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationAdminService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.ArgumentCaptor
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

internal class AuthSecurityEventNotificationAdminControllerTest {
    private val service = mock(IAuthSecurityEventNotificationAdminService::class.java)
    private val controller = AuthSecurityEventNotificationAdminController(service)

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
    fun deadQueryAndReplayPinTenantAndActorFromTheSession() {
        val summary = mock(AuthSecurityEventNotificationSummary::class.java)
        `when`(service.listDead("tenant-1", "event-1", 50)).thenReturn(listOf(summary))
        `when`(service.replay(replayCommand())).thenReturn(summary)

        assertEquals(listOf(summary), controller.listDead(" event-1 ", 50))
        assertEquals(
            summary,
            controller.replay("notification-1", AuthSecurityEventNotificationReplayRequest("channel recovered")),
        )
        val command = ArgumentCaptor.forClass(AuthSecurityEventNotificationReplayCommand::class.java)
        verify(service).replay(command.capture() ?: replayCommand())
        assertEquals("tenant-1", command.value.tenantId)
        assertEquals("admin-1", command.value.actorUserId)
        assertEquals("notification-1", command.value.notificationId)
    }

    @Test
    fun endpointUsesDedicatedPermissionsAndRequiresASession() {
        val listPermission = AuthSecurityEventNotificationAdminController::class.java
            .getDeclaredMethod("listDead", String::class.java, Int::class.javaPrimitiveType)
            .getAnnotation(RequiresPermission::class.java)
        val replayPermission = AuthSecurityEventNotificationAdminController::class.java
            .getDeclaredMethod(
                "replay",
                String::class.java,
                AuthSecurityEventNotificationReplayRequest::class.java,
            ).getAnnotation(RequiresPermission::class.java)
        KudosContextHolder.clear()

        val unauthorized = assertFailsWith<ResponseStatusException> { controller.listDead() }

        assertEquals(401, unauthorized.statusCode.value())
        assertEquals("auth:security-event-notification:view", listPermission.value)
        assertEquals("auth:security-event-notification:replay", replayPermission.value)
        verifyNoInteractions(service)
    }

    @Test
    fun validationAndDomainFailuresUseStableHttpSemantics() {
        val blankEvent = assertFailsWith<ResponseStatusException> { controller.listDead(" ") }
        val invalidLimit = assertFailsWith<ResponseStatusException> { controller.listDead(limit = 501) }
        `when`(service.replay(replayCommand()))
            .thenThrow(
                AuthSecurityEventException("AUTH_SECURITY_EVENT_NOTIFICATION_NOT_FOUND"),
                AuthSecurityEventException("AUTH_SECURITY_EVENT_NOTIFICATION_STATE_CONFLICT"),
            )
        val request = AuthSecurityEventNotificationReplayRequest("channel recovered")
        val missing = assertFailsWith<ResponseStatusException> { controller.replay("notification-1", request) }
        val conflict = assertFailsWith<ResponseStatusException> { controller.replay("notification-1", request) }

        assertEquals(400, blankEvent.statusCode.value())
        assertEquals(400, invalidLimit.statusCode.value())
        assertEquals(404, missing.statusCode.value())
        assertEquals(409, conflict.statusCode.value())
    }

    private fun replayCommand() = AuthSecurityEventNotificationReplayCommand(
        tenantId = "tenant-1",
        notificationId = "notification-1",
        actorUserId = "admin-1",
        reason = "channel recovered",
    )
}
