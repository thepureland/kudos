package io.kudos.ms.auth.core.authentication.mfa.recovery.service.impl

import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeSet
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeStatus
import io.kudos.ms.auth.core.authentication.mfa.recovery.RecoveryCodeErrorCodeEnum
import io.kudos.ms.auth.core.authentication.mfa.recovery.RecoveryCodeException
import io.kudos.ms.auth.core.authentication.mfa.recovery.RecoveryCodeProperties
import io.kudos.ms.auth.core.authentication.mfa.recovery.dao.AuthRecoveryCodeDao
import io.kudos.ms.auth.core.authentication.mfa.recovery.model.po.AuthRecoveryCode
import io.kudos.ms.auth.core.authentication.mfa.recovery.service.iservice.IRecoveryCodeService
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.ITenantMfaPolicyService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.security.IRecoveryCodeVerifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@Service
@Transactional
open class RecoveryCodeService(
    private val dao: AuthRecoveryCodeDao,
    private val userAccountService: IUserAccountService,
    private val properties: RecoveryCodeProperties,
    private val secureRandom: SecureRandom = SecureRandom(),
    private val clock: Clock = Clock.systemUTC(),
    private val mfaPolicyService: ITenantMfaPolicyService? = null,
) : IRecoveryCodeService, IRecoveryCodeVerifier {

    override fun generate(userId: String, tenantId: String): RecoveryCodeSet {
        requireTotp(userId, tenantId)
        requireRecoveryCodesEnabled(tenantId)
        val generatedAt = clock.instant()
        val now = LocalDateTime.ofInstant(generatedAt, ZoneOffset.UTC)
        dao.revokeActive(tenantId, userId, now)
        val setId = UUID.randomUUID().toString()
        val codes = generateSequence(::newCode)
            .distinct()
            .take(properties.codeCount.coerceIn(MIN_CODE_COUNT, MAX_CODE_COUNT))
            .toList()
        codes.forEach { rawCode ->
            dao.insert(AuthRecoveryCode {
                id = UUID.randomUUID().toString()
                this.tenantId = tenantId
                this.userId = userId
                this.setId = setId
                codeHash = hash(tenantId, userId, setId, normalize(rawCode))
                createdAt = now
                consumedAt = null
                revokedAt = null
            })
        }
        return RecoveryCodeSet(codes, generatedAt)
    }

    @Transactional(readOnly = true)
    override fun status(userId: String, tenantId: String): RecoveryCodeStatus {
        ownedAccount(userId, tenantId)
        if (!recoveryCodesEnabled(tenantId)) return RecoveryCodeStatus(enabled = false, remaining = 0)
        val setId = dao.findActiveSetId(tenantId, userId)
            ?: return RecoveryCodeStatus(enabled = false, remaining = 0)
        val remaining = dao.countUnused(tenantId, userId, setId)
        return RecoveryCodeStatus(enabled = remaining > 0, remaining = remaining)
    }

    override fun consume(userId: String, tenantId: String, rawCode: String): Boolean {
        if (!recoveryCodesEnabled(tenantId)) return false
        val setId = dao.findActiveSetId(tenantId, userId) ?: return false
        val normalized = normalize(rawCode)
        if (!isValid(normalized)) return false
        return dao.consume(
            tenantId,
            userId,
            setId,
            hash(tenantId, userId, setId, normalized),
            LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
        )
    }

    override fun consumeRecoveryCode(tenantId: String, userId: String, rawCode: String): Boolean =
        consume(userId = userId, tenantId = tenantId, rawCode = rawCode)

    override fun revoke(userId: String, tenantId: String): Boolean {
        ownedAccount(userId, tenantId)
        return dao.revokeActive(
            tenantId,
            userId,
            LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
        ) > 0
    }

    private fun requireTotp(userId: String, tenantId: String) {
        if (ownedAccount(userId, tenantId).authenticationKey.isNullOrBlank()) {
            fail(RecoveryCodeErrorCodeEnum.MFA_NOT_ENABLED, "An authenticator must be enabled first.")
        }
    }

    private fun requireRecoveryCodesEnabled(tenantId: String) {
        if (!recoveryCodesEnabled(tenantId)) {
            fail(RecoveryCodeErrorCodeEnum.RECOVERY_CODES_DISABLED, "Recovery codes are disabled by tenant policy.")
        }
    }

    private fun recoveryCodesEnabled(tenantId: String): Boolean =
        mfaPolicyService?.getEffective(tenantId)?.recoveryCodesEnabled ?: true

    private fun ownedAccount(userId: String, tenantId: String) =
        userAccountService.get(userId)?.takeIf { it.tenantId == tenantId }
            ?: fail(RecoveryCodeErrorCodeEnum.ACCOUNT_NOT_FOUND, "User account was not found.")

    private fun newCode(): String = buildString(CODE_LENGTH + 3) {
        repeat(CODE_LENGTH) { index ->
            if (index > 0 && index % 4 == 0) append('-')
            append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
        }
    }

    private fun normalize(rawCode: String): String = rawCode
        .trim()
        .replace("-", "")
        .uppercase(Locale.ROOT)

    private fun isValid(code: String): Boolean =
        code.length == CODE_LENGTH && code.all(ALPHABET::contains)

    private fun hash(tenantId: String, userId: String, setId: String, code: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("$tenantId\u0000$userId\u0000$setId\u0000$code".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun fail(code: RecoveryCodeErrorCodeEnum, message: String): Nothing =
        throw RecoveryCodeException(code, message)

    private companion object {
        const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        const val CODE_LENGTH = 16
        const val MIN_CODE_COUNT = 5
        const val MAX_CODE_COUNT = 20
    }
}
