package io.kudos.ms.auth.provider.emailotp

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import io.kudos.ms.auth.provider.emailotp.delivery.EmailOtpDelivery
import io.kudos.ms.auth.provider.emailotp.delivery.IEmailOtpDelivery
import io.kudos.ms.auth.provider.emailotp.identity.IEmailOtpPrincipalService
import io.kudos.ms.auth.provider.emailotp.store.EmailOtpChallenge
import io.kudos.ms.auth.provider.emailotp.store.EmailOtpVerificationResult
import io.kudos.ms.auth.provider.emailotp.store.IEmailOtpChallengeStore
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptContext
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptFactorEnum
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.NoopAuthenticationAttemptLimiter
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Passwordless email ownership proof implemented as a resumable Kudos authentication method. */
open class EmailOtpAuthenticationMethodProvider(
    private val store: IEmailOtpChallengeStore,
    private val delivery: IEmailOtpDelivery,
    private val principalService: IEmailOtpPrincipalService,
    private val properties: EmailOtpProperties,
    private val attemptLimiter: IAuthenticationAttemptLimiter = NoopAuthenticationAttemptLimiter,
    private val random: SecureRandom = SecureRandom(),
) : IAuthenticationMethodProvider {

    init {
        properties.validate()
    }

    override fun method(): String = METHOD_EMAIL_OTP

    override fun supports(transaction: AuthenticationTransaction): Boolean =
        transaction.purpose == AuthenticationTransactionPurposeEnum.LOGIN

    override fun begin(
        transaction: AuthenticationTransaction,
        request: AuthenticationTransactionCreateRequest,
    ): AuthenticationChallenge = if (transaction.tenantId == null) {
        AuthenticationChallenge(
            AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
            AuthenticationActionEnum.SELECT_TENANT,
        )
    } else {
        AuthenticationChallenge(
            AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            AuthenticationActionEnum.VERIFY_EMAIL,
        )
    }

    override fun verify(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationMethodResult {
        if (action != AuthenticationActionEnum.VERIFY_EMAIL) return terminal("EMAIL_OTP_ACTION_INVALID")
        val tenantId = transaction.tenantId
            ?: return challenge(AuthenticationActionEnum.SELECT_TENANT, "TENANT_REQUIRED", null)
        val email = normalizeEmail(request.username ?: transaction.username)
            ?: return challenge(AuthenticationActionEnum.VERIFY_EMAIL, "EMAIL_INVALID", null)
        val context = AuthenticationAttemptContext(tenantId, email, request.loginIp)
        val rawCode = request.code?.trim()?.takeIf(String::isNotEmpty)
        return if (rawCode == null) {
            sendChallenge(transaction, tenantId, email, context)
        } else {
            verifyChallenge(transaction, tenantId, email, rawCode, context)
        }
    }

    private fun sendChallenge(
        transaction: AuthenticationTransaction,
        tenantId: String,
        email: String,
        context: AuthenticationAttemptContext,
    ): AuthenticationMethodResult {
        if (!attemptLimiter.consumeRequest(context).allowed) {
            return challenge(AuthenticationActionEnum.VERIFY_EMAIL, "TOO_MANY_AUTHENTICATION_ATTEMPTS", email)
        }
        val code = generateCode(properties.codeDigits)
        val expiresAt = minOf(
            transaction.expiresAt,
            Instant.now().plus(properties.codeTtlSeconds, ChronoUnit.SECONDS),
        )
        val challenge = EmailOtpChallenge(
            transactionId = transaction.id,
            tenantDigest = sha256(tenantId),
            emailDigest = sha256(email),
            codeDigest = codeDigest(transaction.id, tenantId, email, code),
            expiresAt = expiresAt,
            maxAttempts = properties.maxVerificationAttempts,
        )
        if (!store.issue(challenge)) {
            return challenge(AuthenticationActionEnum.VERIFY_EMAIL, "EMAIL_OTP_ALREADY_SENT", email)
        }
        return try {
            delivery.deliver(EmailOtpDelivery(transaction.id, tenantId, email, code, expiresAt))
            challenge(AuthenticationActionEnum.VERIFY_EMAIL, null, email)
        } catch (_: Exception) {
            store.cancel(transaction.id)
            terminal("EMAIL_OTP_DELIVERY_FAILED", email)
        }
    }

    private fun verifyChallenge(
        transaction: AuthenticationTransaction,
        tenantId: String,
        email: String,
        rawCode: String,
        context: AuthenticationAttemptContext,
    ): AuthenticationMethodResult {
        if (!rawCode.matches(Regex("^\\d{${properties.codeDigits}}$"))) {
            return failedAttempt(context, email, "INVALID_EMAIL_OTP")
        }
        if (!attemptLimiter.checkFailureLimit(context, AuthenticationAttemptFactorEnum.EMAIL_OTP).allowed) {
            return challenge(AuthenticationActionEnum.VERIFY_EMAIL, "TOO_MANY_AUTHENTICATION_ATTEMPTS", email)
        }
        return when (
            store.verify(
                transactionId = transaction.id,
                tenantDigest = sha256(tenantId),
                emailDigest = sha256(email),
                codeDigest = codeDigest(transaction.id, tenantId, email, rawCode),
                now = Instant.now(),
            )
        ) {
            EmailOtpVerificationResult.VERIFIED -> {
                val principal = try {
                    principalService.resolveOrProvision(tenantId, email)
                } catch (_: Exception) {
                    return terminal("EMAIL_OTP_ACCOUNT_UNAVAILABLE", email)
                }
                if (principal.tenantId != tenantId || principal.userId.isBlank() || principal.username.isBlank()) {
                    return terminal("EMAIL_OTP_ACCOUNT_UNAVAILABLE", email)
                }
                attemptLimiter.clearFailures(context, setOf(AuthenticationAttemptFactorEnum.EMAIL_OTP))
                AuthenticationMethodResult(
                    outcome = AuthenticationMethodOutcomeEnum.SUCCESS,
                    userId = principal.userId,
                    tenantId = principal.tenantId,
                    username = principal.username,
                    amr = setOf(METHOD_EMAIL_OTP),
                    acr = ACR_EMAIL_OTP,
                )
            }

            EmailOtpVerificationResult.ATTEMPTS_EXHAUSTED -> {
                attemptLimiter.recordFailure(context, AuthenticationAttemptFactorEnum.EMAIL_OTP)
                challenge(AuthenticationActionEnum.VERIFY_EMAIL, "TOO_MANY_AUTHENTICATION_ATTEMPTS", email)
            }

            EmailOtpVerificationResult.NOT_FOUND,
            EmailOtpVerificationResult.EXPIRED,
            EmailOtpVerificationResult.MISMATCH -> failedAttempt(context, email, "INVALID_EMAIL_OTP")
        }
    }

    private fun failedAttempt(
        context: AuthenticationAttemptContext,
        email: String,
        errorCode: String,
    ): AuthenticationMethodResult {
        attemptLimiter.recordFailure(context, AuthenticationAttemptFactorEnum.EMAIL_OTP)
        return challenge(AuthenticationActionEnum.VERIFY_EMAIL, errorCode, email)
    }

    private fun challenge(action: AuthenticationActionEnum, errorCode: String?, email: String?) =
        AuthenticationMethodResult(
            outcome = AuthenticationMethodOutcomeEnum.CHALLENGE,
            nextAction = action,
            username = email,
            errorCode = errorCode,
        )

    private fun terminal(errorCode: String, email: String? = null) = AuthenticationMethodResult(
        outcome = AuthenticationMethodOutcomeEnum.FAILURE,
        terminal = true,
        username = email,
        errorCode = errorCode,
    )

    private fun normalizeEmail(raw: String?): String? {
        val email = raw?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.length in 3..254 } ?: return null
        return email.takeIf(EMAIL_REGEX::matches)
    }

    private fun generateCode(digits: Int): String {
        val lower = POWERS_OF_TEN[digits - 1]
        val upper = POWERS_OF_TEN[digits]
        return (lower + random.nextInt(upper - lower)).toString()
    }

    private fun codeDigest(transactionId: String, tenantId: String, email: String, code: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(properties.codeHmacSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal("$transactionId\u0000$tenantId\u0000$email\u0000$code".toByteArray(Charsets.UTF_8)).hex()
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).hex()

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        const val METHOD_EMAIL_OTP = "email_otp"
        const val ACR_EMAIL_OTP = "urn:kudos:acr:email-otp"
        val POWERS_OF_TEN = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000)
        val EMAIL_REGEX = Regex(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
        )
    }
}
