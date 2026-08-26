package io.kudos.ms.auth.core.authentication.mfa.policy

import org.springframework.boot.context.properties.ConfigurationProperties

/** Deployment capabilities that affect which mandatory tenant policies can be saved safely. */
@ConfigurationProperties("kudos.ms.auth.mfa.policy")
open class TenantMfaPolicyProperties {
    /** Enable only when the public WebAuthn provider and self-enrollment endpoints are deployed. */
    var allowWebAuthnOnly: Boolean = false

    /**
     * Ceiling for one administrator-granted enrollment exemption, in days.
     *
     * Clamped to 1..30 by the service: the point of the rescue is to get somebody working again this week, and
     * a longer window is the tenant's policy switched off for that account without anyone having to say so.
     */
    var maxEnrollmentExemptionDays: Int = 7
}
