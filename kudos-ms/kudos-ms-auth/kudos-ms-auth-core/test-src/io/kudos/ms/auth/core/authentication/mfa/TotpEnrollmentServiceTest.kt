package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ability.security.common.support.Authenticator
import io.kudos.ms.auth.core.authentication.mfa.service.impl.TotpEnrollmentService
import io.kudos.ms.auth.core.authentication.mfa.store.InMemoryTotpEnrollmentStore
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TotpEnrollmentServiceTest {

    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val store = InMemoryTotpEnrollmentStore(clock)
    private val accounts = mock(IUserAccountService::class.java)
    private val properties = TotpEnrollmentProperties().apply {
        issuer = "Kudos Test"
        enrollmentTtlSeconds = 120
        enrollmentMaxFailedAttempts = 2
    }
    private val service = TotpEnrollmentService(
        store,
        FixedAuthenticator,
        accounts,
        properties,
        clock,
    )

    @Test
    fun beginKeepsOnlyEncryptedPendingSecretAndDoesNotActivateAccount() {
        account()

        val challenge = service.begin("u-1", "t-1", "alice@example.com")
        val pending = store.get(challenge.enrollmentId)!!

        assertEquals(FixedAuthenticator.SECRET, challenge.secret)
        assertTrue(challenge.otpauthUrl.startsWith("otpauth://totp/Kudos+Test%3Aalice%40example.com"))
        assertEquals(now.plusSeconds(120), challenge.expiresAt)
        assertNotEquals(challenge.secret, pending.encryptedSecret)
        verify(accounts, never()).activateVerifiedAuthKey("u-1", FixedAuthenticator.SECRET)
    }

    @Test
    fun validCodeAtomicallyConsumesEnrollmentAndActivatesVerifiedSecret() {
        account()
        `when`(accounts.activateVerifiedAuthKey("u-1", FixedAuthenticator.SECRET)).thenReturn(true)
        val challenge = service.begin("u-1", "t-1", "alice")

        assertTrue(service.confirm(challenge.enrollmentId, "u-1", "t-1", FixedAuthenticator.VALID_CODE))

        assertNull(store.get(challenge.enrollmentId))
        verify(accounts).activateVerifiedAuthKey("u-1", FixedAuthenticator.SECRET)
    }

    @Test
    fun activationConflictFailsClosedAfterConsumingEnrollment() {
        account()
        `when`(accounts.activateVerifiedAuthKey("u-1", FixedAuthenticator.SECRET)).thenReturn(false)
        val challenge = service.begin("u-1", "t-1", "alice")

        val error = assertFailsWith<TotpEnrollmentException> {
            service.confirm(challenge.enrollmentId, "u-1", "t-1", FixedAuthenticator.VALID_CODE)
        }

        assertEquals(TotpEnrollmentErrorCodeEnum.TOTP_ACTIVATION_FAILED, error.errorCode)
        assertNull(store.get(challenge.enrollmentId))
        verify(accounts).activateVerifiedAuthKey("u-1", FixedAuthenticator.SECRET)
    }

    @Test
    fun outOfRangeCodeUsesStableDomainErrorWithoutConsumingAnAttempt() {
        account()
        val challenge = service.begin("u-1", "t-1", "alice")

        val error = assertFailsWith<TotpEnrollmentException> {
            service.confirm(challenge.enrollmentId, "u-1", "t-1", 1_000_000)
        }

        assertEquals(TotpEnrollmentErrorCodeEnum.INVALID_TOTP_CODE, error.errorCode)
        assertEquals(0, store.get(challenge.enrollmentId)?.failedAttempts)
    }

    @Test
    fun invalidCodesAreVersionedAndBounded() {
        account()
        val challenge = service.begin("u-1", "t-1", "alice")

        val first = assertFailsWith<TotpEnrollmentException> {
            service.confirm(challenge.enrollmentId, "u-1", "t-1", 1)
        }
        assertEquals(TotpEnrollmentErrorCodeEnum.INVALID_TOTP_CODE, first.errorCode)

        val second = assertFailsWith<TotpEnrollmentException> {
            service.confirm(challenge.enrollmentId, "u-1", "t-1", 2)
        }
        assertEquals(TotpEnrollmentErrorCodeEnum.TOTP_ATTEMPTS_EXCEEDED, second.errorCode)
        verify(accounts, never()).activateVerifiedAuthKey("u-1", FixedAuthenticator.SECRET)
    }

    @Test
    fun enrollmentCannotCrossUserBoundary() {
        account()
        val challenge = service.begin("u-1", "t-1", "alice")

        val error = assertFailsWith<TotpEnrollmentException> {
            service.confirm(challenge.enrollmentId, "u-2", "t-1", FixedAuthenticator.VALID_CODE)
        }

        assertEquals(TotpEnrollmentErrorCodeEnum.TOTP_ENROLLMENT_NOT_FOUND, error.errorCode)
        assertTrue(store.get(challenge.enrollmentId) != null)
    }

    @Test
    fun existingTotpCannotBeSilentlyRotatedThroughEnrollment() {
        account(authenticationKey = "EXISTINGSECRET22")

        val error = assertFailsWith<TotpEnrollmentException> {
            service.begin("u-1", "t-1", "alice")
        }

        assertEquals(TotpEnrollmentErrorCodeEnum.TOTP_ALREADY_ENABLED, error.errorCode)
    }

    @Test
    fun disableIsIdempotentForAnAccountWithoutTotp() {
        account()

        assertFalse(service.disable("u-1", "t-1"))
        verify(accounts, never()).cleanAuthKey("u-1")
    }

    private fun account(authenticationKey: String? = null) {
        `when`(accounts.get("u-1")).thenReturn(UserAccount {
            id = "u-1"
            tenantId = "t-1"
            username = "alice"
            this.authenticationKey = authenticationKey
        })
    }

    private object FixedAuthenticator : Authenticator {
        const val SECRET = "JBSWY3DPEHPK3PXP"
        const val VALID_CODE = 123456
        override fun generateKey() = SECRET
        override fun verify(secret: String, code: Int) = secret == SECRET && code == VALID_CODE
        override fun generateCode(secret: String) = VALID_CODE.toString()
    }
}
