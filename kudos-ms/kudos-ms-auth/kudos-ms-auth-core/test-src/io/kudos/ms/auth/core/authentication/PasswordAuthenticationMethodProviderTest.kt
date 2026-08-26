package io.kudos.ms.auth.core.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.method.PasswordAuthenticationMethodProvider
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeStatus
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementResult
import io.kudos.ms.auth.core.authentication.mfa.AuthenticationSecondFactorRegistry
import io.kudos.ms.auth.core.authentication.mfa.policy.model.EffectiveTenantMfaPolicy
import io.kudos.ms.auth.core.authentication.mfa.policy.model.MfaPolicyDecision
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Pure mapping tests for the transitional Passport adapter. */
internal class PasswordAuthenticationMethodProviderTest {
    private val passportService = mock(IPassportService::class.java)
    private val provider = PasswordAuthenticationMethodProvider(passportService)

    private fun transaction(tenantId: String? = "t-1") = AuthenticationTransaction(
        id = "tx-1",
        tenantId = tenantId,
        status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
        method = "password",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(300),
    )

    private fun action(code: String? = null) = AuthenticationActionRequest(
        username = "alice",
        plainPassword = "secret",
        code = code,
        loginIp = 0x7F000001L,
        loginDevice = "PC",
    )

    private fun passportRequest(authCode: Long? = null, recoveryCode: String? = null) = PassportLoginRequest(
        tenantId = "t-1",
        username = "alice",
        plainPassword = "secret",
        loginIp = 0x7F000001L,
        authCode = authCode,
        loginDevice = "PC",
        recoveryCode = recoveryCode,
    )

    @Test
    fun begin_withoutTenant_requestsTenantSelection() {
        val challenge = provider.begin(transaction(null), AuthenticationTransactionCreateRequest(requestedMethod = "password"))

        assertEquals(AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION, challenge.status)
        assertEquals(AuthenticationActionEnum.SELECT_TENANT, challenge.nextAction)
    }

    @Test
    fun verify_success_mapsPrincipalAmrAndAcr() {
        val info = UserInfoModel(
            id = "u-1", username = "alice", tenantId = "t-1", orgId = null,
            accountTypeDictCode = null, defaultLocale = null, defaultTimezone = null,
            defaultCurrency = null, loginTime = LocalDateTime.now(),
        )
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.success(info))

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(AuthenticationMethodOutcomeEnum.SUCCESS, result.outcome)
        assertEquals("u-1", result.userId)
        assertEquals(setOf("password"), result.amr)
        assertEquals(PasswordAuthenticationMethodProvider.ACR_PASSWORD, result.acr)
    }

    @Test
    fun verify_otpRequired_returnsTotpChallenge() {
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.otpRequired())

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(AuthenticationMethodOutcomeEnum.CHALLENGE, result.outcome)
        assertEquals(AuthenticationActionEnum.VERIFY_TOTP, result.nextAction)
    }

    @Test
    fun verify_totpSuccess_recordsBothMethods() {
        val info = UserInfoModel(
            id = "u-1", username = "alice", tenantId = "t-1", orgId = null,
            accountTypeDictCode = null, defaultLocale = null, defaultTimezone = null,
            defaultCurrency = null, loginTime = LocalDateTime.now(),
        )
        `when`(passportService.login(passportRequest(123456L))).thenReturn(PassportLoginResult.success(info))

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_TOTP, action("123456"))

        assertEquals(setOf("password", "totp"), result.amr)
        assertEquals(PasswordAuthenticationMethodProvider.ACR_MFA, result.acr)
    }

    @Test
    fun verify_otpChallengeOffersRecoveryOnlyWhenCodesExist() {
        val recoveryCodes = mock(IRecoveryCodeService::class.java)
        val accounts = mock(IUserAccountService::class.java)
        val provider = PasswordAuthenticationMethodProvider(passportService, recoveryCodes, accounts)
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.otpRequired())
        val account = mock(UserAccountCacheEntry::class.java)
        `when`(account.id).thenReturn("u-1")
        `when`(accounts.getUserByTenantIdAndUsername("t-1", "alice")).thenReturn(account)
        `when`(recoveryCodes.status("u-1", "t-1")).thenReturn(RecoveryCodeStatus(true, 5))

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(
            setOf(AuthenticationActionEnum.VERIFY_TOTP, AuthenticationActionEnum.VERIFY_RECOVERY_CODE),
            result.nextActions,
        )
    }

    @Test
    fun verify_recoveryCodeSuccessRecordsMfaMethods() {
        val info = UserInfoModel(
            id = "u-1", username = "alice", tenantId = "t-1", orgId = null,
            accountTypeDictCode = null, defaultLocale = null, defaultTimezone = null,
            defaultCurrency = null, loginTime = LocalDateTime.now(),
        )
        `when`(passportService.login(passportRequest(recoveryCode = "2345-6789-ABCD-EFGH")))
            .thenReturn(PassportLoginResult.success(info))

        val result = provider.verify(
            transaction(),
            AuthenticationActionEnum.VERIFY_RECOVERY_CODE,
            action("2345-6789-ABCD-EFGH"),
        )

        assertEquals(setOf("password", "recovery_code"), result.amr)
        assertEquals(PasswordAuthenticationMethodProvider.ACR_MFA, result.acr)
    }

    @Test
    fun verify_accountRevealingFailures_useTheSameRetryableResult() {
        val passportResults = listOf(
            PassportLoginResult.userNotFound(),
            PassportLoginResult.wrongPassword(3),
            PassportLoginResult.inactive(),
            PassportLoginResult.locked(),
            PassportLoginResult.accountFrozen("Administrative review"),
            PassportLoginResult.invalidCredentials(),
        )

        passportResults.forEach { passportResult ->
            `when`(passportService.login(passportRequest())).thenReturn(passportResult)

            val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

            assertEquals(AuthenticationMethodOutcomeEnum.CHALLENGE, result.outcome)
            assertEquals(AuthenticationActionEnum.VERIFY_PASSWORD, result.nextAction)
            assertEquals("INVALID_CREDENTIALS", result.errorCode)
            assertTrue(!result.terminal)
        }
    }

    @Test
    fun verify_rateLimited_keepsTheTransactionRetryable() {
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.rateLimited(30))

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(AuthenticationMethodOutcomeEnum.CHALLENGE, result.outcome)
        assertEquals(AuthenticationActionEnum.VERIFY_PASSWORD, result.nextAction)
        assertEquals("TOO_MANY_AUTHENTICATION_ATTEMPTS", result.errorCode)
        assertTrue(!result.terminal)
    }

    @Test
    fun stepUp_ignoresClientUsernameAndUsesPinnedInitiatorUsername() {
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.wrongPassword(1))
        val stepUp = transaction().copy(
            purpose = AuthenticationTransactionPurposeEnum.STEP_UP,
            initiatorUserId = "u-1",
            username = "alice",
        )

        provider.verify(
            stepUp,
            AuthenticationActionEnum.VERIFY_PASSWORD,
            action().copy(username = "mallory"),
        )

        verify(passportService).login(passportRequest())
    }

    @Test
    fun requiredEnrollmentDuringGraceCompletesWithPostAuthenticationAction() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val provider = PasswordAuthenticationMethodProvider(
            passportService = passportService,
            mfaPolicyEnforcer = enforcer,
        )
        val info = userInfo()
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.success(info))
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                PasswordAuthenticationMethodProvider.ACR_PASSWORD,
            )
        ).thenReturn(MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED))

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(AuthenticationMethodOutcomeEnum.SUCCESS, result.outcome)
        assertEquals(setOf(AuthenticationActionEnum.ENROLL_MFA), result.postAuthenticationActions)
    }

    @Test
    fun requiredEnrollmentAfterGraceFailsTerminally() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val provider = PasswordAuthenticationMethodProvider(
            passportService = passportService,
            mfaPolicyEnforcer = enforcer,
        )
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.success(userInfo()))
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                PasswordAuthenticationMethodProvider.ACR_PASSWORD,
            )
        ).thenReturn(MfaPolicyEnforcementResult(MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED))

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(AuthenticationMethodOutcomeEnum.FAILURE, result.outcome)
        assertTrue(result.terminal)
        assertEquals("MFA_ENROLLMENT_REQUIRED", result.errorCode)
    }

    @Test
    fun verifiedPasswordCanContinueWithAllowedEnrolledPasskeyWithoutRetainingPassword() {
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val secondFactors = mock(AuthenticationSecondFactorRegistry::class.java)
        val provider = PasswordAuthenticationMethodProvider(
            passportService = passportService,
            mfaPolicyEnforcer = enforcer,
            secondFactorRegistry = secondFactors,
        )
        val allowedMethods = setOf(MfaMethodEnum.WEBAUTHN)
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.success(userInfo()))
        `when`(
            enforcer.enforce(
                AuthenticationTransactionPurposeEnum.LOGIN,
                "t-1",
                "u-1",
                PasswordAuthenticationMethodProvider.ACR_PASSWORD,
            )
        ).thenReturn(
            MfaPolicyEnforcementResult(
                MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR,
                MfaPolicyDecision(
                    policy = EffectiveTenantMfaPolicy("t-1", allowedMethods = allowedMethods),
                    required = true,
                    enrolled = true,
                    enrollmentRequired = false,
                    gracePeriodActive = false,
                    graceExpiresAt = null,
                ),
            )
        )
        `when`(secondFactors.availableActions("u-1", "t-1", allowedMethods)).thenReturn(
            setOf(AuthenticationActionEnum.VERIFY_PASSKEY)
        )

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(AuthenticationMethodOutcomeEnum.CHALLENGE, result.outcome)
        assertEquals(setOf(AuthenticationActionEnum.VERIFY_PASSKEY), result.nextActions)
        assertEquals("u-1", result.userId)
        assertEquals(setOf("password"), result.amr)
        assertEquals(PasswordAuthenticationMethodProvider.ACR_PASSWORD, result.acr)
        assertTrue(!result.toString().contains("secret"))
    }

    @Test
    fun tenantPolicyCanHideRecoveryCodeAlternative() {
        val recoveryCodes = mock(IRecoveryCodeService::class.java)
        val accounts = mock(IUserAccountService::class.java)
        val enforcer = mock(AuthenticationMfaPolicyEnforcer::class.java)
        val provider = PasswordAuthenticationMethodProvider(passportService, recoveryCodes, accounts, enforcer)
        `when`(passportService.login(passportRequest())).thenReturn(PassportLoginResult.otpRequired())
        `when`(enforcer.recoveryCodesEnabled("t-1")).thenReturn(false)

        val result = provider.verify(transaction(), AuthenticationActionEnum.VERIFY_PASSWORD, action())

        assertEquals(setOf(AuthenticationActionEnum.VERIFY_TOTP), result.nextActions)
        verify(recoveryCodes, never()).status("u-1", "t-1")
    }

    private fun userInfo() = UserInfoModel(
        id = "u-1", username = "alice", tenantId = "t-1", orgId = null,
        accountTypeDictCode = null, defaultLocale = null, defaultTimezone = null,
        defaultCurrency = null, loginTime = LocalDateTime.now(),
    )
}
