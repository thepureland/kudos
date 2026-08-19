package io.kudos.ms.auth.common.authz.api

import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import org.springframework.web.service.annotation.PostExchange
import org.springframework.web.bind.annotation.RequestBody

/**
 * Internal RPC contract for the canonical authorization decision point.
 *
 * @author K
 * @author AI: Codex
 */
interface IAuthzDecisionRemoteApi {

    /** Answers one authorization question; remote failure must be handled as DENY by clients. */
    @PostExchange("/api/internal/auth/authz/decide")
    fun decide(@RequestBody request: AuthzRequest): AuthzDecision
}
