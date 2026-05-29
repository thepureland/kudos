package io.kudos.ability.security.jwt.exception

import org.springframework.security.oauth2.jwt.BadJwtException

/**
 * Thrown by [io.kudos.ability.security.jwt.support.JwtExpValidator] when an inbound JWT's `exp`
 * claim is in the past (accounting for the configured clock-skew tolerance).
 *
 * Extends Spring Security's [BadJwtException] so any existing `JwtAuthenticationException` /
 * exception-translator wiring keeps handling it correctly — apps do NOT need to register a
 * special mapper just for kudos's expired-token signal.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class JwtExpiredException : BadJwtException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
