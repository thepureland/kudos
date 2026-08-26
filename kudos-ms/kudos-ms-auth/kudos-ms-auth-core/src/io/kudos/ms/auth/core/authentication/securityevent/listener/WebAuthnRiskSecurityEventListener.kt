package io.kudos.ms.auth.core.authentication.securityevent.listener

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event.WebAuthnAuthenticatorRiskPolicyBlocked
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import org.springframework.context.event.EventListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

/** Persists blocked assertions independently from the authentication transaction that will roll back. */
@Component
open class WebAuthnRiskSecurityEventListener(
    private val securityEventService: IAuthSecurityEventService,
) {
    private val log = LogFactory.getLog(this::class)

    @EventListener
    open fun onBlocked(event: WebAuthnAuthenticatorRiskPolicyBlocked) {
        val command = event.toRecordCommand()
        try {
            securityEventService.recordOrAggregate(command)
        } catch (e: DataIntegrityViolationException) {
            if (!e.isUniqueViolation()) {
                log.error(e, "Failed to persist WebAuthn authenticator risk security event")
                return
            }
            runCatching { securityEventService.aggregateAfterConcurrentInsert(command) }
                .onFailure { log.error(it, "Failed to aggregate concurrent WebAuthn risk security event") }
        } catch (e: Exception) {
            log.error(e, "Failed to persist WebAuthn authenticator risk security event")
        }
    }

    private fun WebAuthnAuthenticatorRiskPolicyBlocked.toRecordCommand(): AuthSecurityEventRecordCommand {
        val sources = riskSources.normalizedCodes()
        val statusCodes = riskStatusCodes.normalizedCodes()
        val occurredAtUtc = LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC)
        val bucketEpochSecond = occurredAt.epochSecond / DEDUPLICATION_BUCKET_SECONDS * DEDUPLICATION_BUCKET_SECONDS
        val bucketStart = LocalDateTime.ofEpochSecond(bucketEpochSecond, 0, ZoneOffset.UTC)
        val deduplicationKey = sha256(
            listOf(userId, credentialIdFingerprint, riskLevel.name, sources.joinToString(","), statusCodes.joinToString(","))
                .joinToString("|") { "${it.length}:$it" }
        )
        return AuthSecurityEventRecordCommand(
            tenantId = tenantId,
            userId = userId,
            eventType = AuthSecurityEventTypeEnum.WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED,
            subjectType = WEBAUTHN_CREDENTIAL_SUBJECT,
            subjectFingerprint = credentialIdFingerprint,
            riskLevel = riskLevel,
            riskSources = sources,
            riskStatusCodes = statusCodes,
            deduplicationKey = deduplicationKey,
            bucketStart = bucketStart,
            occurredAt = occurredAtUtc,
        )
    }

    private fun Set<String>.normalizedCodes(): Set<String> = asSequence()
        .map { value ->
            value.trim().map {
                if (it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in "_.:-") it else '_'
            }.joinToString("")
        }
        .filter { it.isNotBlank() }
        .map { it.take(MAX_CODE_LENGTH) }
        .toSortedSet()
        .take(MAX_CODES_PER_DIMENSION)
        .toSet()

    private fun sha256(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    )

    private fun DataIntegrityViolationException.isUniqueViolation(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is SQLException && (current.sqlState == UNIQUE_VIOLATION_SQL_STATE || current.errorCode == 1062)) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        const val DEDUPLICATION_BUCKET_SECONDS = 300L
        const val MAX_CODE_LENGTH = 64
        const val MAX_CODES_PER_DIMENSION = 16
        const val UNIQUE_VIOLATION_SQL_STATE = "23505"
        const val WEBAUTHN_CREDENTIAL_SUBJECT = "WEBAUTHN_CREDENTIAL"
    }
}
