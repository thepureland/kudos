package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.dao.AuthMfaEnrollmentExemptionDao
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionGrantCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionRevokeCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.MfaEnrollmentExemptionStatusEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.po.AuthMfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.service.impl.MfaEnrollmentExemptionService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.mfa.policy.model.TenantMfaPolicyException
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
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
import kotlin.test.assertNull

internal class MfaEnrollmentExemptionServiceTest {
    private val instant = Instant.parse("2026-08-25T10:00:00Z")
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val dao = mock(AuthMfaEnrollmentExemptionDao::class.java)
    private val policyService = mock(ITenantMfaPolicyService::class.java)
    private val service = MfaEnrollmentExemptionService(dao, policyService, clock, TenantMfaPolicyProperties())

    @Test
    fun grantingRecordsABoundedWindowWithItsOperatorAndReason() {
        `when`(policyService.evaluate("tenant-1", "user-1")).thenReturn(decision(enrolled = false))

        val granted = service.grant(command())

        assertEquals("user-1", granted.userId)
        assertEquals(MfaEnrollmentExemptionStatusEnum.ACTIVE, granted.status)
        assertEquals("admin-1", granted.grantedBy)
        assertEquals("lost phone, replacement issued Friday", granted.reason)
        assertEquals(now, granted.grantedAt)
        assertEquals(now.plusDays(3), granted.expiresAt)
        assertNull(granted.revokedAt)
        val stored = ArgumentCaptor.forClass(AuthMfaEnrollmentExemption::class.java)
        verify(dao).insert(stored.capture() ?: exemptionPo())
        assertEquals("ACTIVE", stored.value.status)
        assertEquals("tenant-1", stored.value.tenantId)
    }

    @Test
    fun anEnrolledAccountCannotBeExemptedFromPresentingItsFactor() {
        `when`(policyService.evaluate("tenant-1", "user-1")).thenReturn(decision(enrolled = true))

        val failure = assertFailsWith<TenantMfaPolicyException> { service.grant(command()) }

        assertEquals("MFA_EXEMPTION_ALREADY_ENROLLED", failure.errorCode)
        verify(dao, never()).insert(any(AuthMfaEnrollmentExemption::class.java) ?: exemptionPo())
    }

    @Test
    fun nobodyExemptsThemselves() {
        val failure = assertFailsWith<TenantMfaPolicyException> {
            service.grant(command(userId = "admin-1"))
        }

        assertEquals("MFA_EXEMPTION_SELF_GRANT_FORBIDDEN", failure.errorCode)
        // Refused before the account is even evaluated: this is about who is asking, not about their state.
        verify(policyService, never()).evaluate("tenant-1", "admin-1")
    }

    @Test
    fun theWindowMustBeInTheFutureAndInsideTheConfiguredCeiling() {
        `when`(policyService.evaluate("tenant-1", "user-1")).thenReturn(decision(enrolled = false))

        val past = assertFailsWith<TenantMfaPolicyException> {
            service.grant(command(expiresAt = now.minusMinutes(1)))
        }
        val tooLong = assertFailsWith<TenantMfaPolicyException> {
            service.grant(command(expiresAt = now.plusDays(8)))
        }

        assertEquals("MFA_EXEMPTION_WINDOW_INVALID", past.errorCode)
        assertEquals("MFA_EXEMPTION_WINDOW_INVALID", tooLong.errorCode)
    }

    @Test
    fun aDeploymentCannotConfigureAnUnboundedRescue() {
        val overreaching = MfaEnrollmentExemptionService(
            dao,
            policyService,
            clock,
            TenantMfaPolicyProperties().apply { maxEnrollmentExemptionDays = 3650 },
        )
        `when`(policyService.evaluate("tenant-1", "user-1")).thenReturn(decision(enrolled = false))

        val failure = assertFailsWith<TenantMfaPolicyException> {
            overreaching.grant(command(expiresAt = now.plusDays(31)))
        }

        assertEquals("MFA_EXEMPTION_WINDOW_INVALID", failure.errorCode)
        // Still accepted right at the hard ceiling, so the clamp is a ceiling and not an off-by-one refusal.
        overreaching.grant(command(expiresAt = now.plusDays(30)))
    }

    @Test
    fun exemptingAnAccountTheTenantPolicyDoesNotRequireIsRefusedAsMeaningless() {
        `when`(policyService.evaluate("tenant-1", "user-1"))
            .thenReturn(decision(enrolled = false, required = false))

        val failure = assertFailsWith<TenantMfaPolicyException> { service.grant(command()) }

        assertEquals("MFA_EXEMPTION_NOT_REQUIRED", failure.errorCode)
    }

    @Test
    fun malformedGrantsAreRefusedBeforeAnythingIsWritten() {
        val cases = mapOf(
            "MFA_EXEMPTION_TENANT_INVALID" to command(tenantId = " "),
            "MFA_EXEMPTION_USER_INVALID" to command(userId = ""),
            "MFA_EXEMPTION_ACTOR_INVALID" to command(actorUserId = "x".repeat(37)),
            "MFA_EXEMPTION_REASON_INVALID" to command(reason = "  "),
        )

        cases.forEach { (errorCode, command) ->
            assertEquals(errorCode, assertFailsWith<TenantMfaPolicyException> { service.grant(command) }.errorCode)
        }
        verify(dao, never()).insert(any(AuthMfaEnrollmentExemption::class.java) ?: exemptionPo())
    }

    @Test
    fun theActiveWindowIsTheLatestOfConcurrentGrantsAndRevokingTakesThemAllAway() {
        `when`(dao.findActive("tenant-1", "user-1", now)).thenReturn(
            listOf(exemptionPo(expires = now.plusDays(1)), exemptionPo(expires = now.plusDays(4))),
        )
        `when`(dao.revokeActive("tenant-1", "user-1", "admin-1", "device returned", now)).thenReturn(2)

        assertEquals(now.plusDays(4), service.activeExemptionExpiry("tenant-1", "user-1"))
        assertEquals(
            2,
            service.revoke(
                MfaEnrollmentExemptionRevokeCommand(
                    tenantId = "tenant-1",
                    userId = "user-1",
                    actorUserId = "admin-1",
                    reason = "device returned",
                )
            ),
        )
    }

    @Test
    fun anAccountWithoutAGrantHasNoWindow() {
        `when`(dao.findActive("tenant-1", "user-1", now)).thenReturn(emptyList())

        assertNull(service.activeExemptionExpiry("tenant-1", "user-1"))
    }

    private fun command(
        tenantId: String = "tenant-1",
        userId: String = "user-1",
        expiresAt: LocalDateTime = now.plusDays(3),
        actorUserId: String = "admin-1",
        reason: String = "lost phone, replacement issued Friday",
    ) = MfaEnrollmentExemptionGrantCommand(tenantId, userId, expiresAt, actorUserId, reason)

    private fun decision(enrolled: Boolean, required: Boolean = true) = MfaPolicyDecision(
        policy = EffectiveTenantMfaPolicy(tenantId = "tenant-1"),
        required = required,
        enrolled = enrolled,
        enrollmentRequired = required && !enrolled,
        gracePeriodActive = false,
        graceExpiresAt = now.minusDays(1),
    )

    private fun exemptionPo(expires: LocalDateTime = now.plusDays(3)) = AuthMfaEnrollmentExemption {
        id = "exemption-1"
        tenantId = "tenant-1"
        userId = "user-1"
        status = "ACTIVE"
        reason = "lost phone"
        grantedBy = "admin-1"
        grantedAt = now
        expiresAt = expires
        revokedBy = null
        revokeReason = null
        revokedAt = null
    }
}
