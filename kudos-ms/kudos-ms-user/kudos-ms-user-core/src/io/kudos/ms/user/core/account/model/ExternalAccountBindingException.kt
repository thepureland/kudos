package io.kudos.ms.user.core.account.model

/** Stable failure returned by the external-account binding lifecycle. */
class ExternalAccountBindingException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalStateException(errorCode, cause)
