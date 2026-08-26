package io.kudos.ms.auth.core.authentication.assurance

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DefaultAuthenticationAssurancePolicyTest {

    private val policy = DefaultAuthenticationAssurancePolicy()

    @Test
    fun knownAcrs_followConservativeStrengthOrdering() {
        assertTrue(policy.isSatisfied(DefaultAuthenticationAssurancePolicy.ACR_MFA, DefaultAuthenticationAssurancePolicy.ACR_PASSWORD))
        assertTrue(policy.isSatisfied(DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT, DefaultAuthenticationAssurancePolicy.ACR_MFA))
        assertTrue(policy.isSatisfied(DefaultAuthenticationAssurancePolicy.ACR_PHISHING_RESISTANT, DefaultAuthenticationAssurancePolicy.ACR_WEBAUTHN))
        assertFalse(policy.isSatisfied(DefaultAuthenticationAssurancePolicy.ACR_WEBAUTHN, DefaultAuthenticationAssurancePolicy.ACR_MFA))
        assertFalse(policy.isSatisfied(DefaultAuthenticationAssurancePolicy.ACR_PASSWORD, DefaultAuthenticationAssurancePolicy.ACR_MFA))
    }

    @Test
    fun unknownAcr_requiresExactMatch() {
        assertTrue(policy.isSatisfied("urn:industry:loa:2", "urn:industry:loa:2"))
        assertFalse(policy.isSatisfied("urn:industry:loa:3", "urn:industry:loa:2"))
        assertFalse(policy.isSatisfied("", DefaultAuthenticationAssurancePolicy.ACR_PASSWORD))
    }
}
