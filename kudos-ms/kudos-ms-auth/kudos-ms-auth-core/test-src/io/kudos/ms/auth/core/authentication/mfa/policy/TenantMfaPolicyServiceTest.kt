package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaRequirementModeEnum
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.authentication.mfa.policy.dao.AuthTenantMfaPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.model.po.AuthTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.service.impl.TenantMfaPolicyService
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.IMfaEnrollmentQuery
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TenantMfaPolicyServiceTest {
    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val dao = mock(AuthTenantMfaPolicyDao::class.java)
    private val accounts = mock(IUserAccountService::class.java)
    private val roles = mock(IAuthRoleService::class.java)
    private val totp = mock(ITotpEnrollmentService::class.java)
    private val service = TenantMfaPolicyService(dao, accounts, roles, totp, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun missingConfigurationIsOptionalAndSecretFreeByDefault() {
        `when`(accounts.get("u-1")).thenReturn(account("STAFF"))

        val decision = service.evaluate("t-1", "u-1")

        assertEquals(MfaRequirementModeEnum.OPTIONAL, decision.policy.mode)
        assertFalse(decision.policy.configured)
        assertFalse(decision.required)
        assertFalse(decision.enrollmentRequired)
        verify(roles, never()).getUserRoles("u-1")
    }

    @Test
    fun conditionalRoleMatchRequiresEnrollmentAndUsesLaterPolicyDateForGrace() {
        val policyTime = LocalDateTime.of(2026, 8, 24, 10, 0)
        `when`(dao.get("t-1")).thenReturn(policy(mode = "CONDITIONAL", roleCodes = "SECURITY_ADMIN", time = policyTime))
        `when`(accounts.get("u-1")).thenReturn(account("STAFF", LocalDateTime.of(2020, 1, 1, 0, 0)))
        `when`(roles.getUserRoles("u-1")).thenReturn(listOf(role("SECURITY_ADMIN")))
        `when`(totp.isEnabled("u-1", "t-1")).thenReturn(false)

        val decision = service.evaluate("t-1", "u-1")

        assertTrue(decision.required)
        assertTrue(decision.enrollmentRequired)
        assertTrue(decision.gracePeriodActive)
        assertEquals(policyTime.plusDays(7), decision.graceExpiresAt)
    }

    @Test
    fun saveNormalizesTypedValuesAndKeepsAuditActor() {
        val result = service.save(command())

        assertTrue(result.configured)
        assertEquals(setOf("ADMIN", "STAFF"), result.requiredAccountTypeCodes)
        assertEquals(setOf("OPS", "SECURITY_ADMIN"), result.requiredRoleCodes)
        val captor = ArgumentCaptor.forClass(AuthTenantMfaPolicy::class.java)
        verify(dao).insert(captor.capture() ?: fallbackPolicy())
        assertEquals("t-1", captor.value.id)
        assertEquals("admin-1", captor.value.updateUserId)
        assertEquals("Enable MFA", captor.value.updateReason)
        assertEquals("TOTP,WEBAUTHN", captor.value.allowedMethods)
    }

    @Test
    fun enforcingPolicyRejectsUnavailableWebauthnOnlyConfiguration() {
        val error = assertFailsWith<TenantMfaPolicyException> {
            service.save(command(allowedMethods = setOf("WEBAUTHN")))
        }

        assertEquals("MFA_POLICY_NO_AVAILABLE_METHOD", error.errorCode)
        verify(dao, never()).insert(any(AuthTenantMfaPolicy::class.java) ?: fallbackPolicy())
    }

    @Test
    fun explicitlyEnabledDeploymentCanSaveWebauthnOnlyMandatoryPolicy() {
        val capableService = TenantMfaPolicyService(
            dao = dao,
            userAccountService = accounts,
            authRoleService = roles,
            totpEnrollmentService = totp,
            clock = Clock.fixed(now, ZoneOffset.UTC),
            properties = TenantMfaPolicyProperties().apply { allowWebAuthnOnly = true },
        )

        val result = capableService.save(command(allowedMethods = setOf("WEBAUTHN")))

        assertEquals(setOf(MfaMethodEnum.WEBAUTHN), result.allowedMethods)
        val captor = ArgumentCaptor.forClass(AuthTenantMfaPolicy::class.java)
        verify(dao).insert(captor.capture() ?: fallbackPolicy())
        assertEquals("WEBAUTHN", captor.value.allowedMethods)
    }

    @Test
    fun effectiveEnrollmentUsesAnyMethodAllowedByTheTenantPolicy() {
        val enrollment = mock(IMfaEnrollmentQuery::class.java)
        val methodAwareService = TenantMfaPolicyService(
            dao,
            accounts,
            roles,
            totp,
            Clock.fixed(now, ZoneOffset.UTC),
            enrollment,
        )
        `when`(dao.get("t-1")).thenReturn(
            policy(mode = "REQUIRED").apply { allowedMethods = "TOTP,WEBAUTHN" }
        )
        `when`(accounts.get("u-1")).thenReturn(account("STAFF"))
        `when`(enrollment.enrolledMethods("u-1", "t-1")).thenReturn(setOf(
            io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum.WEBAUTHN
        ))

        val decision = methodAwareService.evaluate("t-1", "u-1")

        assertTrue(decision.required)
        assertTrue(decision.enrolled)
        assertFalse(decision.enrollmentRequired)
    }

    private fun command(allowedMethods: Set<String> = setOf("totp", "webauthn")) =
        TenantMfaPolicySaveCommand(
            tenantId = "t-1",
            mode = "conditional",
            gracePeriodDays = 7,
            allowedMethods = allowedMethods,
            recoveryCodesEnabled = true,
            requiredAccountTypeCodes = setOf(" STAFF ", "ADMIN"),
            requiredRoleCodes = setOf("SECURITY_ADMIN", " OPS "),
            actorUserId = "admin-1",
            operationReason = " Enable MFA ",
        )

    private fun account(type: String?, createdAt: LocalDateTime = LocalDateTime.of(2026, 8, 20, 0, 0)) =
        UserAccount {
            id = "u-1"
            tenantId = "t-1"
            username = "alice"
            accountTypeDictCode = type
            createTime = createdAt
        }

    private fun policy(
        mode: String,
        roleCodes: String? = null,
        time: LocalDateTime = LocalDateTime.of(2026, 8, 24, 10, 0),
    ) = AuthTenantMfaPolicy {
        id = "t-1"
        tenantId = "t-1"
        this.mode = mode
        gracePeriodDays = 7
        allowedMethods = "TOTP"
        recoveryCodesEnabled = true
        requiredAccountTypeCodes = null
        requiredRoleCodes = roleCodes
        createUserId = "admin-1"
        createReason = "create"
        createTime = time
        updateUserId = "admin-1"
        updateReason = "update"
        updateTime = time
    }

    private fun role(code: String) = AuthRoleCacheEntry(
        id = "r-1", code = code, name = code, tenantId = "t-1", subsysCode = null,
        remark = null, active = true, builtIn = false, createUserId = null,
        createUserName = null, createTime = null, updateUserId = null, updateUserName = null,
        updateTime = null,
    )

    private fun fallbackPolicy() = policy("OPTIONAL")
}
