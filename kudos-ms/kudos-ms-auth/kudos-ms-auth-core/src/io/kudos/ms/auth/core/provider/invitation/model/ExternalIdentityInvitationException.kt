package io.kudos.ms.auth.core.provider.invitation.model

/** Stable failure returned by the external-identity invitation lifecycle. */
class ExternalIdentityInvitationException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalStateException(errorCode, cause)
