package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.EffectiveWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.WebAuthnAttestationPolicySaveCommand

interface IWebAuthnAttestationPolicyService {
    fun getEffective(tenantId: String): EffectiveWebAuthnAttestationPolicy
    fun isTrustedAttestationAvailable(): Boolean
    fun save(command: WebAuthnAttestationPolicySaveCommand): EffectiveWebAuthnAttestationPolicy
    fun enforce(
        tenantId: String,
        aaguid: String,
        attestationFormat: String,
        attestationTrusted: Boolean,
    )
}
