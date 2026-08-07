package io.kudos.ability.security.enforcement.port

import jakarta.servlet.http.HttpServletRequest

/**
 * Supplies server-derived ABAC attributes such as authenticated claims, directory facts, or device
 * posture. Implementations must verify anything originating outside the service trust boundary.
 *
 * @author K
 * @author AI: Codex
 */
fun interface ITrustedAuthorizationContextProvider {
    fun attributes(request: HttpServletRequest): Map<String, Any?>
}
