package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaRequirementModeEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.vo.TenantMfaPolicyAdminSaveRequest
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TenantMfaPolicyAdminControllerTest {
    private val service = mock(ITenantMfaPolicyService::class.java)
    private val controller = TenantMfaPolicyAdminController(service)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(KudosContext().apply {
            user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
        })
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun savePinsTrustedTenantAndOperator() {
        `when`(service.save(any(TenantMfaPolicySaveCommand::class.java) ?: fallbackCommand()))
            .thenReturn(EffectiveTenantMfaPolicy("tenant-1", MfaRequirementModeEnum.REQUIRED, configured = true))

        val response = controller.save(request())

        assertTrue(response.configured)
        val captor = ArgumentCaptor.forClass(TenantMfaPolicySaveCommand::class.java)
        verify(service).save(captor.capture() ?: fallbackCommand())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("admin-1", captor.value.actorUserId)
        assertEquals("Security baseline K-43", captor.value.operationReason)
    }

    @Test
    fun endpointsDeclareConcretePermissions() {
        val get = TenantMfaPolicyAdminController::class.java.getDeclaredMethod("get")
            .getAnnotation(RequiresPermission::class.java)
        val save = TenantMfaPolicyAdminController::class.java
            .getDeclaredMethod("save", TenantMfaPolicyAdminSaveRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)

        assertEquals("auth:mfa-policy:view", get.value)
        assertEquals("auth:mfa-policy:update", save.value)
    }

    private fun request() = TenantMfaPolicyAdminSaveRequest(
        mode = "REQUIRED",
        gracePeriodDays = 14,
        allowedMethods = setOf("TOTP"),
        recoveryCodesEnabled = true,
        reason = "Security baseline K-43",
    )

    private fun fallbackCommand() = TenantMfaPolicySaveCommand(
        tenantId = "fallback",
        mode = "OPTIONAL",
        gracePeriodDays = 7,
        allowedMethods = setOf("TOTP"),
        recoveryCodesEnabled = true,
        requiredAccountTypeCodes = emptySet(),
        requiredRoleCodes = emptySet(),
        actorUserId = "fallback",
        operationReason = "fallback",
    )
}
