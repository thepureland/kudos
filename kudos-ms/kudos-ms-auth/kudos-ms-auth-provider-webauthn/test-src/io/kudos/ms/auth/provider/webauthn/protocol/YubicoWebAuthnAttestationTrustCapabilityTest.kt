package io.kudos.ms.auth.provider.webauthn.protocol

import com.yubico.webauthn.attestation.AttestationTrustSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.ObjectProvider
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class YubicoWebAuthnAttestationTrustCapabilityTest {

    @Test
    fun capabilityIsAvailableOnlyForOneUnambiguousTrustSource() {
        @Suppress("UNCHECKED_CAST")
        val sources = mock(ObjectProvider::class.java) as ObjectProvider<AttestationTrustSource>
        val capability = YubicoWebAuthnAttestationTrustCapability(sources)

        `when`(sources.getIfUnique()).thenReturn(null)
        assertFalse(capability.isAvailable())

        `when`(sources.getIfUnique()).thenReturn(mock(AttestationTrustSource::class.java))
        assertTrue(capability.isAvailable())
    }
}
