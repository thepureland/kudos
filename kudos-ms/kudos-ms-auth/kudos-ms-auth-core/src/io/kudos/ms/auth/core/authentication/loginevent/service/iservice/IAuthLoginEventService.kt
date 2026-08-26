package io.kudos.ms.auth.core.authentication.loginevent.service.iservice

import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventRecordCommand

/** Append-only authentication outcome audit. */
interface IAuthLoginEventService {

    /**
     * Records one authentication outcome.
     *
     * Called after the transaction's terminal state is durable, in its own transaction, and it never rethrows:
     * an audit store outage must not turn a completed authentication into a failed one, nor keep a rejected one
     * from being rejected. A write that fails is logged as an error — the deployment's alarm, not the user's.
     *
     * @return true when a row was written; false when it was suppressed, already present, or failed
     */
    fun record(command: AuthLoginEventRecordCommand): Boolean

    /** The SHA-256 an identifier would be stored under, so callers can search without knowing the digest form. */
    fun identifierHash(identifier: String): String

    fun listRecent(
        tenantId: String,
        userId: String? = null,
        identifier: String? = null,
        successOnly: Boolean? = null,
        limit: Int = 100,
    ): List<AuthLoginEvent>
}
