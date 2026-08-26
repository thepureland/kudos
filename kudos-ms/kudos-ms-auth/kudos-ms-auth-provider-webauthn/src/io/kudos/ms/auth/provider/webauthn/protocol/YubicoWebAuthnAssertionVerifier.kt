package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.springframework.stereotype.Component

/** The only boundary allowed to turn an untrusted browser assertion into verified identity data. */
@Component
@Suppress("DEPRECATION")
open class YubicoWebAuthnAssertionVerifier : IWebAuthnAssertionVerifier {

    override fun verify(request: WebAuthnAssertionVerificationRequest): VerifiedWebAuthnAssertionResult {
        val response = PublicKeyCredential.parseAssertionResponseJson(request.credentialResponseJson)
        val relyingParty = RelyingParty.builder()
            .identity(
                RelyingPartyIdentity.builder()
                    .id(request.rpId)
                    .name(request.rpName)
                    .build()
            )
            .credentialRepository(request.credentialRepository)
            .origins(request.origins)
            .clock(request.clock)
            .build()
        val result = relyingParty.finishAssertion(
            FinishAssertionOptions.builder()
                .request(request.request)
                .response(response)
                .build()
        )
        require(result.isSuccess && result.isSignatureCounterValid) { "WebAuthn assertion was not verified." }
        return VerifiedWebAuthnAssertionResult(
            credentialId = result.credentialId.base64Url,
            userHandle = result.userHandle.base64Url,
            username = result.username,
            signatureCount = result.signatureCount,
            userVerified = result.isUserVerified,
            backupEligible = result.isBackupEligible,
            backedUp = result.isBackedUp,
        )
    }
}
