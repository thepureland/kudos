package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.impl

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.dao.AuthWebAuthnAttestationPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.EffectiveWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAaguidPolicyModeEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicyException
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicySaveCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.po.AuthWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAttestationPolicyService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.IWebAuthnAttestationTrustCapability
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
open class WebAuthnAttestationPolicyService(
    private val dao: AuthWebAuthnAttestationPolicyDao,
    private val clock: Clock = Clock.systemUTC(),
    private val trustCapability: IWebAuthnAttestationTrustCapability? = null,
) : IWebAuthnAttestationPolicyService {

    @Transactional(readOnly = true)
    override fun getEffective(tenantId: String): EffectiveWebAuthnAttestationPolicy {
        requireTenantId(tenantId)
        return dao.get(tenantId)?.toEffective() ?: EffectiveWebAuthnAttestationPolicy(tenantId)
    }

    @Transactional(readOnly = true)
    override fun isTrustedAttestationAvailable(): Boolean = trustCapability?.isAvailable() == true

    override fun save(command: WebAuthnAttestationPolicySaveCommand): EffectiveWebAuthnAttestationPolicy {
        requireTenantId(command.tenantId)
        if (command.actorUserId.isBlank()) fail("WEBAUTHN_ATTESTATION_POLICY_ACTOR_REQUIRED")
        val reason = command.operationReason.trim()
        if (reason.isBlank() || reason.length > 512) fail("WEBAUTHN_ATTESTATION_POLICY_REASON_INVALID")
        val mode = parseMode(command.aaguidMode)
        val aaguids = normalizeAaguids(command.aaguids)
        if ((mode == WebAuthnAaguidPolicyModeEnum.NONE && aaguids.isNotEmpty()) ||
            (mode != WebAuthnAaguidPolicyModeEnum.NONE && aaguids.isEmpty())
        ) {
            fail("WEBAUTHN_AAGUID_POLICY_INVALID")
        }
        val formats = normalizeFormats(command.allowedAttestationFormats)
        if (command.requireTrustedAttestation && !isTrustedAttestationAvailable()) {
            fail("WEBAUTHN_ATTESTATION_TRUST_NOT_AVAILABLE")
        }
        val now = LocalDateTime.now(clock)
        val existing = dao.get(command.tenantId)
        val policy = existing ?: AuthWebAuthnAttestationPolicy {
            id = command.tenantId
            tenantId = command.tenantId
            createUserId = command.actorUserId
            createReason = reason
            createTime = now
        }
        policy.tenantId = command.tenantId
        policy.aaguidMode = mode.name
        policy.aaguids = aaguids.csvOrNull()
        policy.allowedAttestationFormats = formats.csvOrNull()
        policy.requireTrustedAttestation = command.requireTrustedAttestation
        policy.updateUserId = command.actorUserId
        policy.updateReason = reason
        policy.updateTime = now
        if (existing == null) dao.insert(policy) else if (!dao.update(policy)) {
            fail("WEBAUTHN_ATTESTATION_POLICY_UPDATE_FAILED")
        }
        return policy.toEffective()
    }

    @Transactional(readOnly = true)
    override fun enforce(
        tenantId: String,
        aaguid: String,
        attestationFormat: String,
        attestationTrusted: Boolean,
    ) {
        val policy = getEffective(tenantId)
        val normalizedAaguid = normalizeAaguid(aaguid)
        val format = normalizeFormat(attestationFormat)
        if (policy.requireTrustedAttestation && !attestationTrusted) {
            fail("WEBAUTHN_ATTESTATION_UNTRUSTED")
        }
        if (policy.allowedAttestationFormats.isNotEmpty() && format !in policy.allowedAttestationFormats) {
            fail("WEBAUTHN_ATTESTATION_FORMAT_NOT_ALLOWED")
        }
        when (policy.aaguidMode) {
            WebAuthnAaguidPolicyModeEnum.NONE -> Unit
            WebAuthnAaguidPolicyModeEnum.ALLOW_LIST -> if (normalizedAaguid !in policy.aaguids) {
                fail("WEBAUTHN_AAGUID_NOT_ALLOWED")
            }
            WebAuthnAaguidPolicyModeEnum.DENY_LIST -> if (normalizedAaguid in policy.aaguids) {
                fail("WEBAUTHN_AAGUID_DENIED")
            }
        }
    }

    private fun AuthWebAuthnAttestationPolicy.toEffective() = EffectiveWebAuthnAttestationPolicy(
        tenantId = tenantId,
        aaguidMode = parseMode(aaguidMode),
        aaguids = aaguids.csvValues(),
        allowedAttestationFormats = allowedAttestationFormats.csvValues(),
        requireTrustedAttestation = requireTrustedAttestation,
        effectiveFrom = updateTime,
        configured = true,
    )

    private fun parseMode(value: String): WebAuthnAaguidPolicyModeEnum = runCatching {
        WebAuthnAaguidPolicyModeEnum.valueOf(value.trim().uppercase())
    }.getOrElse { fail("WEBAUTHN_AAGUID_POLICY_MODE_INVALID", it) }

    private fun normalizeAaguids(values: Set<String>): Set<String> {
        if (values.size > MAX_AAGUIDS) fail("WEBAUTHN_AAGUID_POLICY_TOO_LARGE")
        return values.map(::normalizeAaguid).toSortedSet()
    }

    private fun normalizeAaguid(value: String): String = runCatching {
        UUID.fromString(value.trim()).toString()
    }.getOrElse { fail("WEBAUTHN_AAGUID_INVALID", it) }

    private fun normalizeFormats(values: Set<String>): Set<String> {
        if (values.size > MAX_FORMATS) fail("WEBAUTHN_ATTESTATION_FORMAT_POLICY_TOO_LARGE")
        return values.map(::normalizeFormat).toSortedSet()
    }

    private fun normalizeFormat(value: String): String = value.trim().lowercase().also {
        if (it.isBlank() || it.length > 32 || !FORMAT.matches(it)) {
            fail("WEBAUTHN_ATTESTATION_FORMAT_INVALID")
        }
    }

    private fun requireTenantId(tenantId: String) {
        if (tenantId.isBlank() || tenantId.length > 36) fail("WEBAUTHN_ATTESTATION_POLICY_TENANT_INVALID")
    }

    private fun Set<String>.csvOrNull(): String? = takeIf { it.isNotEmpty() }?.joinToString(",")
    private fun String?.csvValues(): Set<String> = this?.split(',')?.map { it.trim() }
        ?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet()

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw WebAuthnAttestationPolicyException(errorCode, cause)

    private companion object {
        val FORMAT = Regex("^[a-z0-9_.-]+$")
        const val MAX_AAGUIDS = 512
        const val MAX_FORMATS = 32
    }
}
