package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.attestation.AttestationTrustSource
import com.yubico.webauthn.data.ByteArray as YubicoByteArray
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.nio.ByteBuffer
import java.util.UUID

/** The only boundary allowed to turn untrusted browser registration JSON into verified public-key material. */
@Component
@Suppress("DEPRECATION")
open class YubicoWebAuthnRegistrationVerifier(
    private val attestationTrustSources: ObjectProvider<AttestationTrustSource>? = null,
) : IWebAuthnRegistrationVerifier {

    override fun verify(request: WebAuthnRegistrationVerificationRequest): VerifiedWebAuthnRegistrationResult {
        val response = PublicKeyCredential.parseRegistrationResponseJson(request.credentialResponseJson)
        val relyingPartyBuilder = RelyingParty.builder()
            .identity(
                RelyingPartyIdentity.builder()
                    .id(request.rpId)
                    .name(request.rpName)
                    .build()
            )
            .credentialRepository(request.credentialRepository)
            .origins(request.origins)
            .allowUntrustedAttestation(true)
            .clock(request.clock)
        attestationTrustSources?.getIfUnique()?.let(relyingPartyBuilder::attestationTrustSource)
        val relyingParty = relyingPartyBuilder.build()
        val result = relyingParty.finishRegistration(
            FinishRegistrationOptions.builder()
                .request(request.request)
                .response(response)
                .build()
        )
        return VerifiedWebAuthnRegistrationResult(
            credentialId = result.keyId.id.base64Url,
            publicKeyCose = result.publicKeyCose.base64Url,
            signatureCount = result.signatureCount,
            transports = response.response.transports.mapTo(sortedSetOf()) { it.id },
            aaguid = result.aaguid.toUuidString(),
            attestationFormat = response.response.attestation.format,
            backupEligible = result.isBackupEligible,
            backedUp = result.isBackedUp,
            discoverable = result.isDiscoverable.orElse(false),
            userVerified = result.isUserVerified,
            attestationTrusted = result.isAttestationTrusted,
        )
    }

    private fun YubicoByteArray.toUuidString(): String {
        require(size() == 16) { "Invalid AAGUID length." }
        val buffer = ByteBuffer.wrap(bytes)
        return UUID(buffer.long, buffer.long).toString()
    }
}
