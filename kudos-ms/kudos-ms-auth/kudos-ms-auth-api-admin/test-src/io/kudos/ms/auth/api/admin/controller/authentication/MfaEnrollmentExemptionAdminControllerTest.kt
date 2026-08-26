package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.MfaEnrollmentExemptionAdminGrantRequest
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.MfaEnrollmentExemptionAdminRevokeRequest
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionGrantCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionRevokeCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionStatusEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.iservice.IMfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
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

internal class MfaEnrollmentExemptionAdminControllerTest {
    private val service = mock(IMfaEnrollmentExemptionService::class.java)
    private val userAccountService = mock(IUserAccountService::class.java)
    private val controller = MfaEnrollmentExemptionAdminController(service, userAccountService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
        `when`(userAccountService.getUserRecord("user-1"))
            .thenReturn(UserAccountRow(id = "user-1", tenantId = "tenant-1"))
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun grantAndRevokePinTheTenantAndTheOperatorToTheAdministratorSession() {
        `when`(service.grant(anyGrantCommand())).thenReturn(exemption())
        `when`(service.revoke(anyRevokeCommand())).thenReturn(1)

        val granted = controller.grant(grantRequest())
        val revoked = controller.revoke(MfaEnrollmentExemptionAdminRevokeRequest(" user-1 ", "device returned"))

        assertEquals("user-1", granted.userId)
        assertEquals("ACTIVE", granted.status)
        assertEquals(mapOf("revokedCount" to 1), revoked)
        val grant = ArgumentCaptor.forClass(MfaEnrollmentExemptionGrantCommand::class.java)
        verify(service).grant(grant.capture() ?: anyGrantCommand())
        assertEquals("tenant-1", grant.value.tenantId)
        assertEquals("admin-1", grant.value.actorUserId)
        assertEquals("user-1", grant.value.userId)
        assertEquals(EXPIRES_AT, grant.value.expiresAt)
    }

    @Test
    fun targetsOutsideTheAdministratorsOwnTenantAreReportedAsAbsent() {
        `when`(userAccountService.getUserRecord("outsider"))
            .thenReturn(UserAccountRow(id = "outsider", tenantId = "tenant-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> {
            controller.grant(grantRequest(userId = "outsider"))
        }
        val missing = assertFailsWith<ResponseStatusException> {
            controller.grant(grantRequest(userId = "unknown"))
        }

        assertEquals(404, crossTenant.statusCode.value())
        assertEquals(404, missing.statusCode.value())
        verifyNoInteractions(service)
    }

    @Test
    fun endpointsUseDedicatedPermissionsAndRequireASession() {
        val viewPermission = MfaEnrollmentExemptionAdminController::class.java
            .getDeclaredMethod("list", String::class.java, Int::class.javaPrimitiveType)
            .getAnnotation(RequiresPermission::class.java)
        val grantPermission = MfaEnrollmentExemptionAdminController::class.java
            .getDeclaredMethod("grant", MfaEnrollmentExemptionAdminGrantRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        val revokePermission = MfaEnrollmentExemptionAdminController::class.java
            .getDeclaredMethod("revoke", MfaEnrollmentExemptionAdminRevokeRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        KudosContextHolder.clear()

        val unauthorized = assertFailsWith<ResponseStatusException> { controller.list() }

        assertEquals(401, unauthorized.statusCode.value())
        // Granting and revoking are separate rights: being able to take a rescue away is not being able to give one.
        assertEquals("auth:mfa-exemption:view", viewPermission.value)
        assertEquals("auth:mfa-exemption:grant", grantPermission.value)
        assertEquals("auth:mfa-exemption:revoke", revokePermission.value)
        verifyNoInteractions(service)
    }

    @Test
    fun domainRefusalsKeepTheirOwnHttpSemantics() {
        `when`(service.grant(anyGrantCommand())).thenThrow(
            TenantMfaPolicyException("MFA_EXEMPTION_ALREADY_ENROLLED"),
            TenantMfaPolicyException("MFA_EXEMPTION_SELF_GRANT_FORBIDDEN"),
            TenantMfaPolicyException("MFA_EXEMPTION_WINDOW_INVALID"),
        )

        val enrolled = assertFailsWith<ResponseStatusException> { controller.grant(grantRequest()) }
        val selfGrant = assertFailsWith<ResponseStatusException> { controller.grant(grantRequest()) }
        val window = assertFailsWith<ResponseStatusException> { controller.grant(grantRequest()) }
        val badLimit = assertFailsWith<ResponseStatusException> { controller.list(limit = 201) }

        assertEquals(409, enrolled.statusCode.value())
        assertEquals(403, selfGrant.statusCode.value())
        assertEquals(400, window.statusCode.value())
        assertEquals(400, badLimit.statusCode.value())
    }

    private fun grantRequest(userId: String = " user-1 ") = MfaEnrollmentExemptionAdminGrantRequest(
        userId = userId,
        expiresAt = EXPIRES_AT,
        reason = "lost phone, replacement issued Friday",
    )

    private fun anyGrantCommand() = MfaEnrollmentExemptionGrantCommand(
        tenantId = "tenant-1",
        userId = "user-1",
        expiresAt = EXPIRES_AT,
        actorUserId = "admin-1",
        reason = "lost phone, replacement issued Friday",
    )

    private fun anyRevokeCommand() = MfaEnrollmentExemptionRevokeCommand(
        tenantId = "tenant-1",
        userId = "user-1",
        actorUserId = "admin-1",
        reason = "device returned",
    )

    private fun exemption() = MfaEnrollmentExemption(
        id = "exemption-1",
        tenantId = "tenant-1",
        userId = "user-1",
        status = MfaEnrollmentExemptionStatusEnum.ACTIVE,
        reason = "lost phone, replacement issued Friday",
        grantedBy = "admin-1",
        grantedAt = LocalDateTime.parse("2026-08-25T10:00:00"),
        expiresAt = EXPIRES_AT,
        revokedBy = null,
        revokeReason = null,
        revokedAt = null,
    )

    private companion object {
        val EXPIRES_AT: LocalDateTime = LocalDateTime.parse("2026-08-28T10:00:00")
    }
}
