package io.kudos.ms.auth.core.authentication.mfa.recovery

class RecoveryCodeException(
    val errorCode: RecoveryCodeErrorCodeEnum,
    override val message: String,
) : RuntimeException(message)
