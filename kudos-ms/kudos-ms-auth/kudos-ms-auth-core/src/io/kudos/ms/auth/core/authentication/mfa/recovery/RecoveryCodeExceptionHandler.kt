package io.kudos.ms.auth.core.authentication.mfa.recovery

import io.kudos.base.annotations.IgnoreApiResponseWrap
import io.kudos.ms.auth.common.authentication.vo.RecoveryCodeError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
open class RecoveryCodeExceptionHandler {

    @IgnoreApiResponseWrap
    @ExceptionHandler(RecoveryCodeException::class)
    open fun handle(ex: RecoveryCodeException): ResponseEntity<RecoveryCodeError> =
        ResponseEntity.status(status(ex.errorCode)).body(
            RecoveryCodeError(code = ex.errorCode.name, message = ex.message),
        )

    private fun status(code: RecoveryCodeErrorCodeEnum): HttpStatus = when (code) {
        RecoveryCodeErrorCodeEnum.ACCOUNT_NOT_FOUND -> HttpStatus.NOT_FOUND
        RecoveryCodeErrorCodeEnum.MFA_NOT_ENABLED,
        RecoveryCodeErrorCodeEnum.RECOVERY_CODES_DISABLED -> HttpStatus.CONFLICT
    }
}
