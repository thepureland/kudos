package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.ms.auth.core.authentication.assurance.spi.IAuthenticationAssurancePolicy

/**
 * Conservative built-in ACR ordering.
 *
 * Unknown values satisfy only an exact match. Deployments can replace this policy when they use a
 * tenant or industry-specific assurance taxonomy.
 */
open class DefaultAuthenticationAssurancePolicy : IAuthenticationAssurancePolicy {

    override fun isSatisfied(actualAcr: String, requiredAcr: String): Boolean {
        val actual = actualAcr.trim().lowercase()
        val required = requiredAcr.trim().lowercase()
        if (actual.isEmpty() || required.isEmpty()) return false
        if (actual == required) return true
        val actualRank = RANKS[actual] ?: return false
        val requiredRank = RANKS[required] ?: return false
        return actualRank >= requiredRank
    }

    companion object {
        const val ACR_PASSWORD = "urn:kudos:acr:password"
        const val ACR_FEDERATED = "urn:kudos:acr:federated"
        const val ACR_WEBAUTHN = "urn:kudos:acr:webauthn"
        const val ACR_MFA = "urn:kudos:acr:mfa"
        const val ACR_PHISHING_RESISTANT = "urn:kudos:acr:phishing-resistant"

        private val RANKS = mapOf(
            ACR_PASSWORD to 10,
            ACR_FEDERATED to 10,
            ACR_WEBAUTHN to 10,
            ACR_MFA to 20,
            ACR_PHISHING_RESISTANT to 30,
        )
    }
}
