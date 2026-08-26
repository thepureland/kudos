package io.kudos.ms.auth.core.authentication.mfa.webauthn.service.impl

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.*
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID

@Service
@Transactional
open class WebAuthnCredentialService(
    private val dao: AuthWebAuthnCredentialDao,
    private val userAccountService: IUserAccountService,
    private val clock: Clock = Clock.systemUTC(),
    private val eventPublisher: ApplicationEventPublisher? = null,
    private val authenticatorRiskService: IWebAuthnAuthenticatorRiskService? = null,
) : IWebAuthnCredentialService {

    override fun registerVerified(command: VerifiedWebAuthnCredentialRegistration): WebAuthnCredentialSummary {
        validateAccount(command.tenantId, command.userId)
        val credentialId = requireBase64Url(command.credentialId, "WEBAUTHN_CREDENTIAL_ID_INVALID", 1024)
        val userHandle = requireBase64Url(command.userHandle, "WEBAUTHN_USER_HANDLE_INVALID", 64)
        val publicKey = requireBase64Url(command.publicKeyCose, "WEBAUTHN_PUBLIC_KEY_INVALID", 4096)
        requireSignatureCount(command.signatureCount)
        if (command.backedUp && !command.backupEligible) fail("WEBAUTHN_BACKUP_STATE_INVALID")
        val displayName = normalizeDisplayName(command.displayName)
        val transports = normalizeTransports(command.transports)
        if (dao.findByCredentialId(command.tenantId, credentialId) != null) {
            fail("WEBAUTHN_CREDENTIAL_ALREADY_REGISTERED")
        }
        val now = LocalDateTime.now(clock)
        val credential = AuthWebAuthnCredential {
            id = UUID.randomUUID().toString()
            tenantId = command.tenantId
            userId = command.userId
            this.credentialId = credentialId
            this.userHandle = userHandle
            publicKeyCose = publicKey
            signatureCount = command.signatureCount
            this.transports = transports.csvOrNull()
            aaguid = normalizeAaguid(command.aaguid)
            attestationFormat = normalizeAttestationFormat(command.attestationFormat)
            backupEligible = command.backupEligible
            backedUp = command.backedUp
            discoverable = command.discoverable
            this.displayName = displayName
            createdAt = now
            lastUsedAt = null
            revokedAt = null
            version = 0
        }
        dao.insert(credential)
        publishCredentialChanged(command.userId, command.tenantId)
        return credential.toSummary()
    }

    @Transactional(readOnly = true)
    override fun findActive(tenantId: String, credentialId: String): AuthWebAuthnCredential? {
        requireScope(tenantId, credentialId)
        return dao.findActiveByCredentialId(tenantId, credentialId)
    }

    @Transactional(readOnly = true)
    override fun findActiveUserIdByUserHandle(tenantId: String, userHandle: String): String? {
        requireScope(tenantId, userHandle)
        val normalizedHandle = requireBase64Url(userHandle, "WEBAUTHN_USER_HANDLE_INVALID", 64)
        val userIds = dao.findActiveByUserHandle(tenantId, normalizedHandle).map { it.userId }.distinct()
        if (userIds.size > 1) fail("WEBAUTHN_USER_HANDLE_AMBIGUOUS")
        return userIds.singleOrNull()
    }

    @Transactional(readOnly = true)
    override fun listActive(tenantId: String, userId: String): List<WebAuthnCredentialSummary> {
        requireScope(tenantId, userId)
        return dao.findActiveByUser(tenantId, userId).map { it.toSummary() }
    }

    @Transactional(readOnly = true)
    override fun listForAudit(tenantId: String, userId: String): List<WebAuthnCredentialAuditSummary> {
        requireScope(tenantId, userId)
        return dao.findByUser(tenantId, userId)
            .sortedByDescending { it.createdAt }
            .map { it.toAuditSummary(authenticatorRiskService?.evaluate(it.aaguid) ?: WebAuthnAuthenticatorRiskSummary()) }
    }

    @Transactional(readOnly = true)
    override fun isEnrolled(userId: String, tenantId: String): Boolean {
        requireScope(tenantId, userId)
        return dao.hasActive(tenantId, userId)
    }

    override fun recordVerifiedAssertion(command: VerifiedWebAuthnAssertion): WebAuthnCredentialSummary {
        requireScope(command.tenantId, command.userId)
        val credentialId = requireBase64Url(command.credentialId, "WEBAUTHN_CREDENTIAL_ID_INVALID", 1024)
        requireSignatureCount(command.signatureCount)
        val credential = dao.findActiveByCredentialId(command.tenantId, credentialId)
            ?: fail("WEBAUTHN_CREDENTIAL_NOT_FOUND")
        if (credential.userId != command.userId) fail("WEBAUTHN_CREDENTIAL_SUBJECT_MISMATCH")
        if (command.backedUp && !credential.backupEligible) fail("WEBAUTHN_BACKUP_STATE_INVALID")
        if (!validCounterTransition(credential.signatureCount, command.signatureCount)) {
            fail("WEBAUTHN_SIGNATURE_COUNTER_REPLAY")
        }
        val usedAt = LocalDateTime.now(clock)
        if (!dao.updateAssertionState(
                command.tenantId,
                command.userId,
                credentialId,
                credential.signatureCount,
                command.signatureCount,
                command.backedUp,
                usedAt,
            )
        ) {
            fail("WEBAUTHN_CREDENTIAL_CONCURRENTLY_CHANGED")
        }
        credential.signatureCount = command.signatureCount
        credential.backedUp = command.backedUp
        credential.lastUsedAt = usedAt
        return credential.toSummary()
    }

    override fun revoke(tenantId: String, userId: String, credentialId: String): Boolean {
        requireScope(tenantId, userId)
        val normalizedId = requireBase64Url(credentialId, "WEBAUTHN_CREDENTIAL_ID_INVALID", 1024)
        return dao.revoke(tenantId, userId, normalizedId, LocalDateTime.now(clock)).also { revoked ->
            if (revoked) publishCredentialChanged(userId, tenantId)
        }
    }

    override fun rename(
        tenantId: String,
        userId: String,
        credentialId: String,
        displayName: String,
    ): WebAuthnCredentialSummary {
        requireScope(tenantId, userId)
        val normalizedId = requireBase64Url(credentialId, "WEBAUTHN_CREDENTIAL_ID_INVALID", 1024)
        val normalizedName = normalizeDisplayName(displayName)
        val credential = dao.findActiveByCredentialId(tenantId, normalizedId)
            ?.takeIf { it.userId == userId }
            ?: fail("WEBAUTHN_CREDENTIAL_NOT_FOUND")
        if (credential.displayName == normalizedName) return credential.toSummary()
        if (!dao.rename(tenantId, userId, normalizedId, normalizedName)) {
            fail("WEBAUTHN_CREDENTIAL_CONCURRENTLY_CHANGED")
        }
        credential.displayName = normalizedName
        return credential.toSummary()
    }

    private fun validateAccount(tenantId: String, userId: String) {
        requireScope(tenantId, userId)
        val account = userAccountService.get(userId) ?: fail("WEBAUTHN_ACCOUNT_NOT_FOUND")
        if (account.tenantId != tenantId) fail("WEBAUTHN_ACCOUNT_TENANT_MISMATCH")
    }

    private fun publishCredentialChanged(userId: String, tenantId: String) {
        eventPublisher?.publishEvent(
            UserAuthenticationInvalidated(
                id = userId,
                tenantId = tenantId,
                reason = UserAuthenticationInvalidated.Reason.WEBAUTHN_CREDENTIAL_CHANGED,
            )
        )
    }

    private fun requireScope(tenantId: String, value: String) {
        if (tenantId.isBlank() || tenantId.length > 36 || value.isBlank()) fail("WEBAUTHN_SCOPE_INVALID")
    }

    private fun requireBase64Url(value: String, errorCode: String, maxDecodedBytes: Int): String {
        if (value.isBlank() || value.length > maxDecodedBytes * 2 || !BASE64URL.matches(value)) fail(errorCode)
        val decoded = runCatching { Base64.getUrlDecoder().decode(value) }.getOrElse { fail(errorCode, it) }
        if (decoded.isEmpty() || decoded.size > maxDecodedBytes) fail(errorCode)
        return value
    }

    private fun normalizeTransports(values: Set<String>): Set<String> = values.map { raw ->
        raw.trim().lowercase().also { if (it !in SUPPORTED_TRANSPORTS) fail("WEBAUTHN_TRANSPORT_INVALID") }
    }.toSortedSet()

    private fun normalizeAaguid(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
        runCatching { UUID.fromString(raw).toString() }.getOrElse { fail("WEBAUTHN_AAGUID_INVALID", it) }
    }

    private fun normalizeAttestationFormat(value: String?): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.also {
            if (it.length > 32 || !ATTESTATION_FORMAT.matches(it)) fail("WEBAUTHN_ATTESTATION_FORMAT_INVALID")
        }

    private fun normalizeDisplayName(value: String): String = value.trim().also {
        if (it.isBlank() || it.length > 100 || it.any(Character::isISOControl)) {
            fail("WEBAUTHN_DISPLAY_NAME_INVALID")
        }
    }

    private fun requireSignatureCount(value: Long) {
        if (value !in 0..MAX_SIGNATURE_COUNT) fail("WEBAUTHN_SIGNATURE_COUNTER_INVALID")
    }

    private fun validCounterTransition(stored: Long, received: Long): Boolean =
        stored == 0L && received == 0L || received > stored

    private fun AuthWebAuthnCredential.toSummary() = WebAuthnCredentialSummary(
        id = id,
        credentialId = credentialId,
        transports = transports.csvValues(),
        aaguid = aaguid,
        backupEligible = backupEligible,
        backedUp = backedUp,
        discoverable = discoverable,
        displayName = displayName,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
    )

    private fun AuthWebAuthnCredential.toAuditSummary(risk: WebAuthnAuthenticatorRiskSummary) =
        WebAuthnCredentialAuditSummary(
            id = id,
            credentialIdFingerprint = credentialFingerprint(credentialId),
            transports = transports.csvValues(),
            aaguid = aaguid,
            attestationFormat = attestationFormat,
            backupEligible = backupEligible,
            backedUp = backedUp,
            discoverable = discoverable,
            displayName = displayName,
            createdAt = createdAt,
            lastUsedAt = lastUsedAt,
            revokedAt = revokedAt,
            authenticatorRiskLevel = risk.level,
            authenticatorRiskSources = risk.sources,
            authenticatorRiskStatusCodes = risk.statusCodes,
        )

    private fun credentialFingerprint(credentialId: String): String {
        return runCatching { WebAuthnCredentialFingerprints.sha256(credentialId) }
            .getOrElse { fail("WEBAUTHN_CREDENTIAL_ID_INVALID", it) }
    }

    private fun Set<String>.csvOrNull(): String? = takeIf { it.isNotEmpty() }?.joinToString(",")
    private fun String?.csvValues(): Set<String> = this?.split(',')?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet()
    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw WebAuthnCredentialException(errorCode, cause)

    private companion object {
        val BASE64URL = Regex("^[A-Za-z0-9_-]+$")
        val ATTESTATION_FORMAT = Regex("^[A-Za-z0-9_.-]+$")
        val SUPPORTED_TRANSPORTS = setOf("ble", "hybrid", "internal", "nfc", "usb")
        const val MAX_SIGNATURE_COUNT = 4_294_967_295L
    }

}
