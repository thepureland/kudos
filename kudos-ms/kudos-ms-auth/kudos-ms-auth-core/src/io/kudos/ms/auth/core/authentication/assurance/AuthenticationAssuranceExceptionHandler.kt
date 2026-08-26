package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.base.annotations.IgnoreApiResponseWrap
import io.kudos.ms.auth.common.authentication.vo.AuthenticationAssuranceChallenge
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Converts assurance failures into one machine-readable HTTP Step-up challenge contract. */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RestControllerAdvice
open class AuthenticationAssuranceExceptionHandler {

    @IgnoreApiResponseWrap
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AuthenticationAssuranceRequiredException::class)
    open fun handle(ex: AuthenticationAssuranceRequiredException): AuthenticationAssuranceChallenge =
        AuthenticationAssuranceChallenge(
            reason = ex.reason.name,
            requiredAcr = ex.requiredAcr,
            maxAgeSeconds = ex.maxAgeSeconds,
        )
}
