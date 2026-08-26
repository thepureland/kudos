package io.kudos.ms.auth.core.authentication.loginevent.model

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import java.time.LocalDateTime

/** Server-observed client facts; never taken from a request body the client controls. */
data class AuthLoginEventObservation(
    val loginIp: Long? = null,
    val loginLocation: String? = null,
    val loginDevice: String? = null,
    val loginBrowser: String? = null,
    val loginOs: String? = null,
    val userAgent: String? = null,
) {
    companion object {
        val EMPTY = AuthLoginEventObservation()
    }
}

/**
 * One authentication outcome to record.
 *
 * [identifier] is the raw attempted identifier and is hashed before it reaches storage — it is present here
 * only so the service can normalise and digest it in one place. A failed attempt against an account that does
 * not exist still produces a row, because "somebody tried this name and it is not one of ours" is exactly the
 * signal an audit trail exists for.
 */
data class AuthLoginEventRecordCommand(
    val transactionId: String,
    val purpose: AuthenticationTransactionPurposeEnum,
    val success: Boolean,
    val tenantId: String? = null,
    val userId: String? = null,
    val identifier: String? = null,
    val providerId: String? = null,
    val authenticationMethod: String? = null,
    val sessionId: String? = null,
    val failureCode: String? = null,
    val acr: String? = null,
    val amr: Set<String> = emptySet(),
    val observation: AuthLoginEventObservation = AuthLoginEventObservation.EMPTY,
    val occurredAt: LocalDateTime,
)

/** Audit projection; the raw identifier is not recoverable from it. */
data class AuthLoginEvent(
    val id: String,
    val tenantId: String?,
    val userId: String?,
    val identifierHash: String?,
    val providerId: String?,
    val authenticationMethod: String?,
    val transactionId: String,
    val purpose: AuthenticationTransactionPurposeEnum,
    val sessionId: String?,
    val success: Boolean,
    val failureCode: String?,
    val acr: String?,
    val amr: Set<String>,
    val loginIp: Long?,
    val loginLocation: String?,
    val loginDevice: String?,
    val loginBrowser: String?,
    val loginOs: String?,
    val userAgent: String?,
    val occurredAt: LocalDateTime,
)

class AuthLoginEventException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
