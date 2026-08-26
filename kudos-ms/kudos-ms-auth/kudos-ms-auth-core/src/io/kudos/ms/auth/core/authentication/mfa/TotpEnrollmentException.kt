package io.kudos.ms.auth.core.authentication.mfa

class TotpEnrollmentException(
    val errorCode: TotpEnrollmentErrorCodeEnum,
    override val message: String,
) : RuntimeException(message)
