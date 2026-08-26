package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeStatus
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptContext
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptDecision
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptFactorEnum
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptLimiter
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FederatedSecondFactorVerifierTest {
    private val accounts = mock(IUserAccountService::class.java)
    private val recoveryCodes = mock(IRecoveryCodeService::class.java)
    private val limiter = mock(IAuthenticationAttemptLimiter::class.java)
    private val verifier = FederatedSecondFactorVerifier(accounts, recoveryCodes, limiter)
    private val context = AuthenticationAttemptContext("t-1", "alice", 0x7F000001L)

    @Test
    fun validTotpCompletesAndClearsOnlyTotpFailures() {
        `when`(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.TOTP))
            .thenReturn(AuthenticationAttemptDecision.ALLOWED)
        `when`(accounts.verifyAuthCode("u-1", 123456L)).thenReturn(true)

        val result = verifier.verify(
            "u-1", "t-1", "alice", AuthenticationActionEnum.VERIFY_TOTP,
            AuthenticationActionRequest(code = "123456", loginIp = 0x7F000001L),
        )

        assertTrue(result.success)
        assertEquals("totp", result.method)
        verify(limiter).clearFailures(context, setOf(AuthenticationAttemptFactorEnum.TOTP))
    }

    @Test
    fun invalidRecoveryCodeConsumesOnlyRecoveryFailureBucket() {
        `when`(limiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.RECOVERY_CODE))
            .thenReturn(AuthenticationAttemptDecision.ALLOWED)
        `when`(recoveryCodes.consume("u-1", "t-1", "2345-6789-ABCD-EFGH")).thenReturn(false)

        val result = verifier.verify(
            "u-1", "t-1", "alice", AuthenticationActionEnum.VERIFY_RECOVERY_CODE,
            AuthenticationActionRequest(code = "2345-6789-ABCD-EFGH", loginIp = 0x7F000001L),
        )

        assertFalse(result.success)
        assertEquals("INVALID_RECOVERY_CODE", result.errorCode)
        verify(limiter).recordFailure(context, AuthenticationAttemptFactorEnum.RECOVERY_CODE)
        verify(limiter, never()).recordFailure(context, AuthenticationAttemptFactorEnum.TOTP)
    }

    @Test
    fun availableActionsExposeRecoveryOnlyWhenUsableCodesRemain() {
        `when`(recoveryCodes.status("u-1", "t-1")).thenReturn(RecoveryCodeStatus(true, 3))

        val actions = verifier.availableActions("u-1", "t-1")

        assertEquals(
            setOf(AuthenticationActionEnum.VERIFY_TOTP, AuthenticationActionEnum.VERIFY_RECOVERY_CODE),
            actions,
        )
    }
}
