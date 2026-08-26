package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.attestation.AttestationTrustSource
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.IWebAuthnAttestationTrustCapability
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/** Reports whether this deployment has one unambiguous Yubico attestation trust source. */
@Component
@ConditionalOnProperty(prefix = "kudos.ms.auth.webauthn", name = ["enabled"], havingValue = "true")
open class YubicoWebAuthnAttestationTrustCapability(
    private val trustSources: ObjectProvider<AttestationTrustSource>,
) : IWebAuthnAttestationTrustCapability {
    override fun isAvailable(): Boolean = trustSources.getIfUnique() != null
}
