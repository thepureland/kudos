package io.kudos.ms.auth.core.authentication.mfa.recovery

import io.kudos.ms.auth.core.authentication.mfa.recovery.dao.AuthRecoveryCodeDao
import io.kudos.ms.auth.core.authentication.mfa.recovery.model.po.AuthRecoveryCode
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.impl.RecoveryCodeService
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
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

internal class RecoveryCodeServiceTest {

    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val dao = mock(AuthRecoveryCodeDao::class.java)
    private val accounts = mock(IUserAccountService::class.java)
    private val properties = RecoveryCodeProperties().apply { codeCount = 5 }
    private val service = RecoveryCodeService(
        dao,
        accounts,
        properties,
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun generationReturnsUniqueCodesOnceAndPersistsOnlyScopedHashes() {
        account(authenticationKey = "JBSWY3DPEHPK3PXP")

        val generated = service.generate("u-1", "t-1")

        assertEquals(5, generated.codes.size)
        assertEquals(5, generated.codes.toSet().size)
        assertEquals(now, generated.generatedAt)
        assertTrue(generated.codes.all { it.matches(Regex("[2-9A-HJ-NP-Z]{4}(-[2-9A-HJ-NP-Z]{4}){3}")) })
        verify(dao).revokeActive("t-1", "u-1", LocalDateTime.ofInstant(now, ZoneOffset.UTC))
        val captor = ArgumentCaptor.forClass(AuthRecoveryCode::class.java)
        verify(dao, times(5)).insert(capture(captor))
        val stored = captor.allValues
        assertEquals(1, stored.map { it.setId }.toSet().size)
        assertTrue(stored.all { it.codeHash.length == 64 })
        assertTrue(stored.none { record -> generated.codes.any { it == record.codeHash } })
    }

    @Test
    fun generationRequiresAnExistingTotpAuthenticator() {
        account(authenticationKey = null)

        val error = assertFailsWith<RecoveryCodeException> {
            service.generate("u-1", "t-1")
        }

        assertEquals(RecoveryCodeErrorCodeEnum.MFA_NOT_ENABLED, error.errorCode)
        verify(dao, never()).insert(anyCode())
    }

    @Test
    fun submittedCodeIsNormalizedAndConsumedByItsStoredHash() {
        account(authenticationKey = "JBSWY3DPEHPK3PXP")
        val generated = service.generate("u-1", "t-1")
        val captor = ArgumentCaptor.forClass(AuthRecoveryCode::class.java)
        verify(dao, times(5)).insert(capture(captor))
        val stored = captor.allValues.first()
        `when`(dao.findActiveSetId("t-1", "u-1")).thenReturn(stored.setId)
        `when`(
            dao.consume(
                "t-1",
                "u-1",
                stored.setId,
                stored.codeHash,
                LocalDateTime.ofInstant(now, ZoneOffset.UTC),
            )
        ).thenReturn(true)

        assertTrue(service.consume("u-1", "t-1", generated.codes.first().lowercase()))
    }

    @Test
    fun malformedOrMissingSetFailsWithoutAStorageMutation() {
        `when`(dao.findActiveSetId("t-1", "u-1")).thenReturn("set-1")

        assertFalse(service.consume("u-1", "t-1", "not-a-recovery-code"))
        verify(dao, never()).consume(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            anyDateTime(),
        )
    }

    @Test
    fun disabledTenantPolicyRejectsGenerationAndHidesExistingCodes() {
        val policies = mock(ITenantMfaPolicyService::class.java)
        val policyAwareService = RecoveryCodeService(
            dao = dao,
            userAccountService = accounts,
            properties = properties,
            clock = Clock.fixed(now, ZoneOffset.UTC),
            mfaPolicyService = policies,
        )
        account(authenticationKey = "JBSWY3DPEHPK3PXP")
        `when`(policies.getEffective("t-1")).thenReturn(
            EffectiveTenantMfaPolicy(tenantId = "t-1", recoveryCodesEnabled = false, configured = true)
        )

        val error = assertFailsWith<RecoveryCodeException> {
            policyAwareService.generate("u-1", "t-1")
        }

        assertEquals(RecoveryCodeErrorCodeEnum.RECOVERY_CODES_DISABLED, error.errorCode)
        assertEquals(0, policyAwareService.status("u-1", "t-1").remaining)
        assertFalse(policyAwareService.consume("u-1", "t-1", "2345-6789-ABCD-EFGH"))
        verify(dao, never()).findActiveSetId("t-1", "u-1")
    }

    private fun account(authenticationKey: String?) {
        `when`(accounts.get("u-1")).thenReturn(UserAccount {
            id = "u-1"
            tenantId = "t-1"
            username = "alice"
            this.authenticationKey = authenticationKey
        })
    }

    private fun anyCode(): AuthRecoveryCode =
        ArgumentMatchers.any(AuthRecoveryCode::class.java) ?: fallbackCode()

    private fun capture(captor: ArgumentCaptor<AuthRecoveryCode>): AuthRecoveryCode =
        captor.capture() ?: fallbackCode()

    private fun anyDateTime(): LocalDateTime =
        ArgumentMatchers.any(LocalDateTime::class.java) ?: LocalDateTime.ofInstant(now, ZoneOffset.UTC)

    private fun fallbackCode(): AuthRecoveryCode = AuthRecoveryCode {
        id = "fallback"
        tenantId = "t-1"
        userId = "u-1"
        setId = "set-1"
        codeHash = "0".repeat(64)
        createdAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC)
        consumedAt = null
        revokedAt = null
    }
}
