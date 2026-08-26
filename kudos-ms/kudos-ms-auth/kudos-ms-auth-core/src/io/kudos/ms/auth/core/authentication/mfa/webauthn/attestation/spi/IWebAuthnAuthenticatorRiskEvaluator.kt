package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi

/**
 * Assesses an authenticator model without coupling the auth core to a metadata vendor.
 * Implementations must use local or already-verified state and must not perform network I/O here.
 *
 * @author K
 * @author AI: Codex
 */
interface IWebAuthnAuthenticatorRiskEvaluator {

    /** Stable identifier returned by administrator audit APIs. */
    val source: String

    /** Returns this source's current assessment for a normalized AAGUID. */
    fun evaluate(aaguid: String): WebAuthnAuthenticatorRiskAssessment
}

/**
 * One risk source's bounded answer. Status codes are provider-defined, non-secret identifiers.
 *
 * @author K
 * @author AI: Codex
 */
data class WebAuthnAuthenticatorRiskAssessment(
    val level: WebAuthnAuthenticatorRiskLevelEnum,
    val statusCodes: Set<String> = emptySet(),
)

/** Aggregated result returned to policy and audit consumers. */
data class WebAuthnAuthenticatorRiskSummary(
    val level: WebAuthnAuthenticatorRiskLevelEnum = WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED,
    val sources: Set<String> = emptySet(),
    val statusCodes: Set<String> = emptySet(),
)

/** Ordered from least to most severe for deterministic multi-source aggregation. */
enum class WebAuthnAuthenticatorRiskLevelEnum {
    NOT_EVALUATED,
    NORMAL,
    WARNING,
    CRITICAL,
}
