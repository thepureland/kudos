package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.event.WebAuthnAuthenticatorRiskPolicyBlocked
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.metrics.WebAuthnAuthenticatorRiskPolicyMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

internal class WebAuthnAuthenticatorRiskPolicyMetricsTest {

    @Test
    fun countsBlockedAssertionsWithRiskLevelAsTheOnlyTag() {
        val registry = SimpleMeterRegistry()
        val metrics = WebAuthnAuthenticatorRiskPolicyMetrics(registry)

        metrics.onBlocked(event(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL))
        metrics.onBlocked(event(WebAuthnAuthenticatorRiskLevelEnum.CRITICAL))
        metrics.onBlocked(event(WebAuthnAuthenticatorRiskLevelEnum.WARNING))

        assertEquals(2.0, count(registry, "CRITICAL"))
        assertEquals(1.0, count(registry, "WARNING"))
        assertEquals(0.0, count(registry, "NOT_EVALUATED"))
        assertEquals(3, registry.find(WebAuthnAuthenticatorRiskPolicyMetrics.METRIC_NAME).counters().size)
        assertEquals(
            setOf("level"),
            registry.find(WebAuthnAuthenticatorRiskPolicyMetrics.METRIC_NAME).counters()
                .flatMap { it.id.tags }.map { it.key }.toSet(),
        )
    }

    private fun count(registry: SimpleMeterRegistry, level: String): Double =
        registry.get(WebAuthnAuthenticatorRiskPolicyMetrics.METRIC_NAME)
            .tag("level", level)
            .counter()
            .count()

    private fun event(level: WebAuthnAuthenticatorRiskLevelEnum) = WebAuthnAuthenticatorRiskPolicyBlocked(
        tenantId = "tenant-1",
        userId = "user-1",
        credentialIdFingerprint = "A".repeat(43),
        riskLevel = level,
        riskSources = setOf("FIDO_MDS"),
        riskStatusCodes = setOf("REVOKED"),
        occurredAt = Instant.parse("2026-08-25T10:00:00Z"),
    )
}
