package io.kudos.ms.auth.client.authz.proxy

import io.kudos.ms.auth.client.authz.fallback.AuthzDecisionFallback
import io.kudos.ms.auth.common.authz.api.IAuthzDecisionRemoteApi
import org.springframework.cloud.openfeign.FeignClient

/**
 * Feign client for the unified authorization decision contract.
 *
 * @author K
 * @author AI: Codex
 */
@FeignClient(
    name = "auth-role",
    contextId = "authzDecision",
    fallback = AuthzDecisionFallback::class,
)
interface IAuthzDecisionProxy : IAuthzDecisionRemoteApi
