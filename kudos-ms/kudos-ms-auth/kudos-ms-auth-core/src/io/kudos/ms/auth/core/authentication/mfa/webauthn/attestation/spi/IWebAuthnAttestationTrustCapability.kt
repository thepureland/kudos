package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi

/** Deployment capability used to prevent saving an unsatisfiable trusted-attestation policy. */
fun interface IWebAuthnAttestationTrustCapability {
    fun isAvailable(): Boolean
}
