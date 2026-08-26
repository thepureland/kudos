package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventOnCallRosterAdminSaveRequest
import io.kudos.ms.auth.common.authentication.securityevent.vo.AuthSecurityEventOnCallShiftAdminRequest
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRoster
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShift
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShiftCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
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

internal class AuthSecurityEventOnCallRosterAdminControllerTest {
    private val service = mock(IAuthSecurityEventOnCallRosterService::class.java)
    private val userAccountService = mock(IUserAccountService::class.java)
    private val controller = AuthSecurityEventOnCallRosterAdminController(service, userAccountService)

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
        `when`(service.listByTenant("tenant-1")).thenReturn(listOf(roster()))
        `when`(userAccountService.getUserRecord("primary-1")).thenReturn(record("tenant-1"))
        `when`(service.save(anySaveCommand())).thenReturn(roster())

        val listed = controller.list().single()
        val saved = controller.save(request())

        assertEquals("SECURITY", listed.rosterCode)
        assertEquals(listOf("primary-1"), listed.shifts.map { it.responderUserId })
        assertEquals(2L, saved.configVersion)
        val command = ArgumentCaptor.forClass(AuthSecurityEventOnCallRosterSaveCommand::class.java)
        verify(service).save(command.capture() ?: anySaveCommand())
        assertEquals("tenant-1", command.value.tenantId)
        assertEquals("admin-1", command.value.actorUserId)
        assertEquals(listOf("primary-1"), command.value.shifts.map { it.responderUserId })
        assertEquals(3L, command.value.expectedVersion)
    }

    @Test
    fun respondersOutsideTheAdministratorsOwnTenantAreReportedAsAbsent() {
        `when`(userAccountService.getUserRecord("primary-1")).thenReturn(record("tenant-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.save(request()) }

        assertEquals(404, crossTenant.statusCode.value())
        verifyNoInteractions(service)
    }

    @Test
    fun endpointsUseDedicatedPermissionsAndRequireASession() {
        val listPermission = AuthSecurityEventOnCallRosterAdminController::class.java
            .getDeclaredMethod("list")
            .getAnnotation(RequiresPermission::class.java)
        val savePermission = AuthSecurityEventOnCallRosterAdminController::class.java
            .getDeclaredMethod("save", AuthSecurityEventOnCallRosterAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        KudosContextHolder.clear()

        val unauthorized = assertFailsWith<ResponseStatusException> { controller.list() }

        assertEquals(401, unauthorized.statusCode.value())
        assertEquals("auth:security-event-oncall:view", listPermission.value)
        assertEquals("auth:security-event-oncall:update", savePermission.value)
        verifyNoInteractions(service)
    }

    @Test
    fun staleVersionsConflictWhileOversizedOrBlankRotationsAreRejected() {
        `when`(userAccountService.getUserRecord("primary-1")).thenReturn(record("tenant-1"))
        `when`(service.save(anySaveCommand()))
            .thenThrow(AuthSecurityEventException("AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT"))

        val conflict = assertFailsWith<ResponseStatusException> { controller.save(request()) }
        val blankResponder = assertFailsWith<ResponseStatusException> {
            controller.save(request(shifts = listOf(shiftRequest(" "))))
        }
        val tooMany = assertFailsWith<ResponseStatusException> {
            controller.save(request(shifts = (1..101).map { shiftRequest("responder-$it") }))
        }

        assertEquals(409, conflict.statusCode.value())
        assertEquals(400, blankResponder.statusCode.value())
        assertEquals(400, tooMany.statusCode.value())
    }

    private fun request(shifts: List<AuthSecurityEventOnCallShiftAdminRequest> = listOf(shiftRequest(" primary-1 "))) =
        AuthSecurityEventOnCallRosterAdminSaveRequest(
            rosterCode = "SECURITY",
            displayName = "Security duty",
            enabled = true,
            shifts = shifts,
            expectedVersion = 3L,
            reason = "weekly rotation",
        )

    private fun shiftRequest(responderUserId: String) = AuthSecurityEventOnCallShiftAdminRequest(
        responderUserId = responderUserId,
        tier = 1,
        startAt = NOW,
        endAt = NOW.plusHours(8),
    )

    private fun anySaveCommand() = AuthSecurityEventOnCallRosterSaveCommand(
        tenantId = "tenant-1",
        rosterCode = "SECURITY",
        displayName = "Security duty",
        enabled = true,
        // Matches what the controller builds from the default request: the id is trimmed, nothing else changes.
        shifts = listOf(AuthSecurityEventOnCallShiftCommand("primary-1", 1, NOW, NOW.plusHours(8))),
        expectedVersion = 3L,
        actorUserId = "admin-1",
        reason = "weekly rotation",
    )

    private fun roster() = AuthSecurityEventOnCallRoster(
        tenantId = "tenant-1",
        rosterCode = "SECURITY",
        displayName = "Security duty",
        enabled = true,
        shifts = listOf(
            AuthSecurityEventOnCallShift(
                id = "shift-1",
                responderUserId = "primary-1",
                tier = 1,
                startAt = NOW,
                endAt = NOW.plusHours(8),
            )
        ),
        configVersion = 2L,
        createUserId = "admin-1",
        createReason = "initial",
        createTime = NOW,
        updateUserId = "admin-1",
        updateReason = "weekly rotation",
        updateTime = NOW,
    )

    private fun record(tenantId: String) = UserAccountRow(id = "primary-1", tenantId = tenantId)

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.parse("2026-08-25T10:00:00")
    }
}
