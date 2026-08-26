package io.kudos.ms.auth.core.authentication.mfa.service.impl

import io.kudos.ability.security.common.support.Authenticator
import io.kudos.base.security.CryptoKit
import io.kudos.ms.auth.common.authentication.vo.TotpEnrollmentChallenge
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollment
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollmentErrorCodeEnum
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollmentException
import io.kudos.ms.auth.core.authentication.mfa.TotpEnrollmentProperties
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.authentication.mfa.store.ITotpEnrollmentStore
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.Clock
import java.util.UUID

@Service
open class TotpEnrollmentService(
    private val store: ITotpEnrollmentStore,
    private val authenticator: Authenticator,
    private val userAccountService: IUserAccountService,
    private val properties: TotpEnrollmentProperties,
    private val clock: Clock = Clock.systemUTC(),
) : ITotpEnrollmentService {

    override fun begin(userId: String, tenantId: String, accountName: String): TotpEnrollmentChallenge {
        val account = ownedAccount(userId, tenantId)
        if (!account.authenticationKey.isNullOrBlank()) {
            fail(TotpEnrollmentErrorCodeEnum.TOTP_ALREADY_ENABLED, "TOTP is already enabled.")
        }
        val secret = authenticator.generateKey()
        val issuer = properties.issuer.trim().ifEmpty { "Kudos" }
        val now = clock.instant()
        val enrollment = TotpEnrollment(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            userId = userId,
            encryptedSecret = CryptoKit.aesEncrypt(secret),
            createdAt = now,
            expiresAt = now.plusSeconds(properties.enrollmentTtlSeconds.coerceAtLeast(30)),
        )
        check(store.create(enrollment)) { "Generated duplicate TOTP enrollment identifier" }
        val label = URLEncoder.encode("$issuer:$accountName", Charsets.UTF_8)
        val encodedIssuer = URLEncoder.encode(issuer, Charsets.UTF_8)
        return TotpEnrollmentChallenge(
            enrollmentId = enrollment.id,
            secret = secret,
            otpauthUrl = "otpauth://totp/$label?secret=$secret&issuer=$encodedIssuer",
            expiresAt = enrollment.expiresAt,
        )
    }

    override fun confirm(enrollmentId: String, userId: String, tenantId: String, code: Int): Boolean {
        if (code !in 0..999_999) {
            fail(TotpEnrollmentErrorCodeEnum.INVALID_TOTP_CODE, "The TOTP code is invalid.")
        }
        val enrollment = ownedEnrollment(enrollmentId, userId, tenantId)
        val maxAttempts = properties.enrollmentMaxFailedAttempts.coerceAtLeast(1)
        if (enrollment.failedAttempts >= maxAttempts) {
            fail(TotpEnrollmentErrorCodeEnum.TOTP_ATTEMPTS_EXCEEDED, "TOTP enrollment attempts are exhausted.")
        }
        val secret = CryptoKit.aesDecrypt(enrollment.encryptedSecret)
        check(secret.isNotBlank()) { "Pending TOTP secret could not be decrypted" }
        if (!authenticator.verify(secret, code)) {
            val attempts = enrollment.failedAttempts + 1
            store.save(enrollment.copy(failedAttempts = attempts), enrollment.version)
                ?: notFound()
            if (attempts >= maxAttempts) {
                fail(TotpEnrollmentErrorCodeEnum.TOTP_ATTEMPTS_EXCEEDED, "TOTP enrollment attempts are exhausted.")
            }
            fail(TotpEnrollmentErrorCodeEnum.INVALID_TOTP_CODE, "The TOTP code is invalid.")
        }
        store.consume(enrollment.id, enrollment.version) ?: notFound()
        if (!userAccountService.activateVerifiedAuthKey(userId, secret)) {
            fail(TotpEnrollmentErrorCodeEnum.TOTP_ACTIVATION_FAILED, "TOTP could not be activated.")
        }
        return true
    }

    override fun cancel(enrollmentId: String, userId: String, tenantId: String): Boolean {
        val enrollment = ownedEnrollment(enrollmentId, userId, tenantId)
        return store.consume(enrollment.id, enrollment.version) != null
    }

    override fun isEnabled(userId: String, tenantId: String): Boolean =
        !ownedAccount(userId, tenantId).authenticationKey.isNullOrBlank()

    override fun disable(userId: String, tenantId: String): Boolean {
        if (ownedAccount(userId, tenantId).authenticationKey.isNullOrBlank()) return false
        return userAccountService.cleanAuthKey(userId)
    }

    private fun ownedAccount(userId: String, tenantId: String) =
        userAccountService.get(userId)?.takeIf { it.tenantId == tenantId }
            ?: fail(TotpEnrollmentErrorCodeEnum.ACCOUNT_NOT_FOUND, "User account was not found.")

    private fun ownedEnrollment(enrollmentId: String, userId: String, tenantId: String): TotpEnrollment {
        val enrollment = store.get(enrollmentId) ?: notFound()
        if (enrollment.userId != userId || enrollment.tenantId != tenantId) notFound()
        if (!clock.instant().isBefore(enrollment.expiresAt)) {
            store.consume(enrollment.id, enrollment.version)
            fail(TotpEnrollmentErrorCodeEnum.TOTP_ENROLLMENT_EXPIRED, "TOTP enrollment has expired.")
        }
        return enrollment
    }

    private fun notFound(): Nothing =
        fail(TotpEnrollmentErrorCodeEnum.TOTP_ENROLLMENT_NOT_FOUND, "TOTP enrollment was not found.")

    private fun fail(code: TotpEnrollmentErrorCodeEnum, message: String): Nothing =
        throw TotpEnrollmentException(code, message)
}
