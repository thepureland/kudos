package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.service.iservice.IWebAuthnAuthenticatorRiskService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.IWebAuthnAuthenticatorRiskEvaluator
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskAssessment
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskSummary
import org.springframework.stereotype.Service

/** Aggregates local, side-effect-free authenticator risk sources for audit and enforcement. */
@Service
open class WebAuthnAuthenticatorRiskService(
    private val evaluators: List<IWebAuthnAuthenticatorRiskEvaluator> = emptyList(),
) : IWebAuthnAuthenticatorRiskService {
    private val log = LogFactory.getLog(this::class)

    override fun evaluate(aaguid: String?): WebAuthnAuthenticatorRiskSummary {
        if (aaguid == null || evaluators.isEmpty()) return WebAuthnAuthenticatorRiskSummary()
        val assessments = evaluators.map { evaluator ->
            try {
                evaluator.source to evaluator.evaluate(aaguid)
            } catch (e: Exception) {
                log.error(e, "WebAuthn authenticator risk evaluator {0} failed", evaluator.source)
                evaluator.source to WebAuthnAuthenticatorRiskAssessment(
                    level = WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
                    statusCodes = setOf("EVALUATION_FAILED"),
                )
            }
        }
        return WebAuthnAuthenticatorRiskSummary(
            level = assessments.maxOf { it.second.level },
            sources = assessments.map { it.first }.toSortedSet(),
            statusCodes = assessments.flatMap { it.second.statusCodes }.toSortedSet(),
        )
    }

    override fun isAvailable(): Boolean = evaluators.isNotEmpty()
}
