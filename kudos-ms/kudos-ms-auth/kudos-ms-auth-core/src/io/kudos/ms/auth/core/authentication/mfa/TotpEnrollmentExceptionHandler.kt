package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.base.annotations.IgnoreApiResponseWrap
import io.kudos.ms.auth.common.authentication.vo.TotpEnrollmentError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
open class TotpEnrollmentExceptionHandler {

    @IgnoreApiResponseWrap
    @ExceptionHandler(TotpEnrollmentException::class)
    open fun handle(ex: TotpEnrollmentException): ResponseEntity<TotpEnrollmentError> =
        ResponseEntity.status(status(ex.errorCode)).body(
            TotpEnrollmentError(code = ex.errorCode.name, message = ex.message),
        )

    private fun status(code: TotpEnrollmentErrorCodeEnum): HttpStatus = when (code) {
        TotpEnrollmentErrorCodeEnum.ACCOUNT_NOT_FOUND,
        TotpEnrollmentErrorCodeEnum.TOTP_ENROLLMENT_NOT_FOUND,
        -> HttpStatus.NOT_FOUND
        TotpEnrollmentErrorCodeEnum.TOTP_ENROLLMENT_EXPIRED -> HttpStatus.GONE
        TotpEnrollmentErrorCodeEnum.INVALID_TOTP_CODE -> HttpStatus.BAD_REQUEST
        TotpEnrollmentErrorCodeEnum.TOTP_ATTEMPTS_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS
        TotpEnrollmentErrorCodeEnum.TOTP_ALREADY_ENABLED,
        TotpEnrollmentErrorCodeEnum.TOTP_ACTIVATION_FAILED,
        -> HttpStatus.CONFLICT
    }
}
