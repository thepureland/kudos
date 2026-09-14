package io.kudos.ms.auth.provider.emailotp

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.provider.emailotp.delivery.EmailOtpDelivery
import io.kudos.ms.auth.provider.emailotp.delivery.IEmailOtpDelivery
import io.kudos.ms.auth.provider.emailotp.identity.EmailOtpPrincipal
import io.kudos.ms.auth.provider.emailotp.identity.IEmailOtpPrincipalService
import io.kudos.ms.auth.provider.emailotp.store.InMemoryEmailOtpChallengeStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class EmailOtpAuthenticationMethodProviderTest {
    private val deliveries = mutableListOf<EmailOtpDelivery>()
    private var principalCalls = 0
    private val properties = EmailOtpProperties().apply {
        enabled = true
        codeHmacSecret = "0123456789abcdef0123456789abcdef"
        maxVerificationAttempts = 3
    }
    private val provider = EmailOtpAuthenticationMethodProvider(
        store = InMemoryEmailOtpChallengeStore(),
        delivery = IEmailOtpDelivery(deliveries::add),
        principalService = IEmailOtpPrincipalService { tenantId, email ->
            principalCalls++
            EmailOtpPrincipal("user-1", tenantId, "email-user")
        },
        properties = properties,
    )

    @Test
    fun `begin requires tenant before email verification`() {
        val challenge = provider.begin(transaction(tenantId = null), AuthenticationTransactionCreateRequest())

        assertEquals(AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION, challenge.status)
        assertEquals(AuthenticationActionEnum.SELECT_TENANT, challenge.nextAction)
    }

    @Test
    fun `send then verify consumes the code and resolves principal only after proof`() {
        val transaction = transaction()
        val sent = provider.verify(
            transaction,
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = " Player@Example.com "),
        )

        assertEquals(AuthenticationMethodOutcomeEnum.CHALLENGE, sent.outcome)
        assertEquals("player@example.com", sent.username)
        assertEquals(0, principalCalls)
        val delivery = assertNotNull(deliveries.singleOrNull())
        assertEquals("player@example.com", delivery.email)
        assertTrue(delivery.code.matches(Regex("^\\d{6}$")))

        val verified = provider.verify(
            transaction.copy(username = sent.username),
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = delivery.email, code = delivery.code),
        )
        assertEquals(AuthenticationMethodOutcomeEnum.SUCCESS, verified.outcome)
        assertEquals("user-1", verified.userId)
        assertEquals(setOf("email_otp"), verified.amr)
        assertEquals("urn:kudos:acr:email-otp", verified.acr)
        assertEquals(1, principalCalls)

        val replay = provider.verify(
            transaction.copy(username = sent.username),
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = delivery.email, code = delivery.code),
        )
        assertEquals(AuthenticationMethodOutcomeEnum.CHALLENGE, replay.outcome)
        assertEquals("INVALID_EMAIL_OTP", replay.errorCode)
        assertEquals(1, principalCalls)
    }

    @Test
    fun `wrong email and code share one bounded non-enumerating failure`() {
        val transaction = transaction()
        provider.verify(
            transaction,
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = "player@example.com"),
        )
        val code = deliveries.single().code

        val wrongEmail = provider.verify(
            transaction,
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = "other@example.com", code = code),
        )
        val wrongCode = provider.verify(
            transaction,
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = "player@example.com", code = "000000"),
        )
        val exhausted = provider.verify(
            transaction,
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = "player@example.com", code = "111111"),
        )

        assertEquals("INVALID_EMAIL_OTP", wrongEmail.errorCode)
        assertEquals("INVALID_EMAIL_OTP", wrongCode.errorCode)
        assertEquals("TOO_MANY_AUTHENTICATION_ATTEMPTS", exhausted.errorCode)
        assertEquals(0, principalCalls)
    }

    @Test
    fun `delivery failure cancels challenge and terminates safely`() {
        val failing = EmailOtpAuthenticationMethodProvider(
            InMemoryEmailOtpChallengeStore(),
            IEmailOtpDelivery { error("smtp unavailable") },
            IEmailOtpPrincipalService { tenantId, _ -> EmailOtpPrincipal("u", tenantId, "u") },
            properties,
        )
        val result = failing.verify(
            transaction(),
            AuthenticationActionEnum.VERIFY_EMAIL,
            AuthenticationActionRequest(username = "player@example.com"),
        )

        assertEquals(AuthenticationMethodOutcomeEnum.FAILURE, result.outcome)
        assertTrue(result.terminal)
        assertEquals("EMAIL_OTP_DELIVERY_FAILED", result.errorCode)
    }

    @Test
    fun `enabled provider rejects missing hmac secret`() {
        assertFailsWith<IllegalArgumentException> {
            EmailOtpAuthenticationMethodProvider(
                InMemoryEmailOtpChallengeStore(),
                IEmailOtpDelivery {},
                IEmailOtpPrincipalService { tenantId, _ -> EmailOtpPrincipal("u", tenantId, "u") },
                EmailOtpProperties(),
            )
        }
    }

    private fun transaction(tenantId: String? = "tenant-1") = AuthenticationTransaction(
        id = "transaction-1",
        tenantId = tenantId,
        status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
        nextActions = setOf(AuthenticationActionEnum.VERIFY_EMAIL),
        method = "email_otp",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(300),
    )
}
