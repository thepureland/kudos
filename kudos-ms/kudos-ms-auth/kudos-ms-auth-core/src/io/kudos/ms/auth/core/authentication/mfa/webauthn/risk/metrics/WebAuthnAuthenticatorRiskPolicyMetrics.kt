package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.metrics

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event.WebAuthnAuthenticatorRiskPolicyBlocked
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.event.EventListener

/** Low-cardinality security counter; tenant, user, device and vendor status stay in the event only. */
open class WebAuthnAuthenticatorRiskPolicyMetrics(registry: MeterRegistry) {
    private val blockedCounters: Map<WebAuthnAuthenticatorRiskLevelEnum, Counter> = BLOCKABLE_LEVELS.associateWith {
        Counter.builder(METRIC_NAME)
            .description("WebAuthn assertions blocked by tenant authenticator risk policy")
            .tag("level", it.name)
            .register(registry)
    }

    @EventListener
    open fun onBlocked(event: WebAuthnAuthenticatorRiskPolicyBlocked) {
        blockedCounters[event.riskLevel]?.increment()
    }

    companion object {
        const val METRIC_NAME = "kudos.auth.webauthn.risk.policy.blocked"
        private val BLOCKABLE_LEVELS = setOf(
            WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
            WebAuthnAuthenticatorRiskLevelEnum.WARNING,
            WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        )
    }
}
