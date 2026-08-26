package io.kudos.ms.user.core.account.model

/** Stable failure returned by external-account JIT provisioning. */
class ExternalAccountProvisioningException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalStateException(errorCode, cause)
