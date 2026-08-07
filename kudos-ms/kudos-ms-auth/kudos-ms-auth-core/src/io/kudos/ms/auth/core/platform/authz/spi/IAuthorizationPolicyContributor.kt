package io.kudos.ms.auth.core.platform.authz.spi

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import org.springframework.stereotype.Component

/**
 * Adds a bounded custom policy to the canonical decision algebra.
 *
 * The built-in model covers roles, groups, attributes, data scopes, instance grants, delegation,
 * approval and explicit DENY. A deployment contributes this SPI only for rules that cannot be
 * represented by those common concepts. Ownership and other subject-object relationships are one
 * example, but the contract deliberately does not prescribe an industry or a relationship model.
 *
 * @author K
 * @author AI: Codex
 */
fun interface IAuthorizationPolicyContributor {

    /** Return false to avoid loading facts for requests this contributor does not understand. */
    fun supports(request: AuthzRequest): Boolean = true

    /** Must be deterministic, side-effect free and fast enough for the authorization hot path. */
    fun evaluate(request: AuthzRequest): AuthorizationPolicyContribution
}

/**
 * One contributor's answer. DENY has precedence when all contributions are merged.
 *
 * @author K
 * @author AI: Codex
 */
data class AuthorizationPolicyContribution(
    val effect: Effect,
    val policyCode: String? = null,
    val detail: String? = null,
) {
    /**
     * @author K
     * @author AI: Codex
     */
    enum class Effect { ABSTAIN, ALLOW, DENY }

    companion object {
        fun abstain() = AuthorizationPolicyContribution(Effect.ABSTAIN)
        fun allow(policyCode: String, detail: String? = null) =
            AuthorizationPolicyContribution(Effect.ALLOW, policyCode, detail)
        fun deny(policyCode: String, detail: String? = null) =
            AuthorizationPolicyContribution(Effect.DENY, policyCode, detail)
    }
}

/**
 * Executes ordered contributors and converts provider failures into DENY contributions.
 * An unavailable extension must never silently remove a restriction.
 *
 * @author K
 * @author AI: Codex
 */
@Component
open class AuthorizationPolicyExtensionEvaluator(
    private val contributors: List<IAuthorizationPolicyContributor> = emptyList(),
) {
    private val log = LogFactory.getLog(this::class)

    open fun evaluate(request: AuthzRequest): List<AuthorizationPolicyContribution> =
        contributors.mapNotNull { contributor ->
            try {
                if (!contributor.supports(request)) return@mapNotNull null
                contributor.evaluate(request).takeUnless {
                    it.effect == AuthorizationPolicyContribution.Effect.ABSTAIN
                }
            } catch (e: Exception) {
                log.error(e, "Authorization policy contributor {0} failed", contributor.javaClass.name)
                AuthorizationPolicyContribution.deny(
                    policyCode = "extension-unavailable",
                    detail = "an authorization policy extension could not be evaluated",
                )
            }
        }

}
