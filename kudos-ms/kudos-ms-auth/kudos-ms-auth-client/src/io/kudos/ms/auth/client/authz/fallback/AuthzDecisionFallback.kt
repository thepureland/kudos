package io.kudos.ms.auth.client.authz.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.auth.client.authz.proxy.IAuthzDecisionProxy
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest

/**
 * Fail-closed degradation path: an unreachable authorization service never grants access.
 *
 * @author K
 * @author AI: Codex
 */
open class AuthzDecisionFallback :
    AbstractHttpFallbackSupport("AuthzDecisionFallback"), IAuthzDecisionProxy {

    override fun decide(request: AuthzRequest): AuthzDecision {
        warnRead("decide", request.subject.principalId, request.permissionCode)
        return AuthzDecision.deny(
            AuthzDecision.Reason.DENIED_BY_DEFAULT,
            code = request.permissionCode,
            detail = "authorization service unavailable; denied by fail-closed fallback",
        )
    }
}
