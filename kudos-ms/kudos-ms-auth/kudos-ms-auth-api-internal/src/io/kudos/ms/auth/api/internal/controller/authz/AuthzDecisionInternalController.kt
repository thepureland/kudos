package io.kudos.ms.auth.api.internal.controller.authz

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.api.IAuthzDecisionRemoteApi
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import org.springframework.web.bind.annotation.RestController

/**
 * Internal HTTP adapter for the canonical decision point.
 *
 * @author K
 * @author AI: Codex
 */
@RestController
class AuthzDecisionInternalController(
    private val decisionApi: IAuthzDecisionApi,
) : IAuthzDecisionRemoteApi {

    override fun decide(request: AuthzRequest): AuthzDecision = decisionApi.decide(request)
}
