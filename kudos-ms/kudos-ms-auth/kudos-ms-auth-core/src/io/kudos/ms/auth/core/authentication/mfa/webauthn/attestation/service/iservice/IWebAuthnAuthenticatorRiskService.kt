package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskSummary

interface IWebAuthnAuthenticatorRiskService {
    fun evaluate(aaguid: String?): WebAuthnAuthenticatorRiskSummary

    fun isAvailable(): Boolean
}
