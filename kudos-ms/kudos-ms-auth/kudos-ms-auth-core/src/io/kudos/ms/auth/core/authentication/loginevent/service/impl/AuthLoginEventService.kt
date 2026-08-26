package io.kudos.ms.auth.core.authentication.loginevent.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.core.authentication.loginevent.dao.AuthLoginEventDao
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventException
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventRecordCommand
import io.kudos.ms.auth.core.authentication.loginevent.model.po.AuthLoginEvent as AuthLoginEventPo
import io.kudos.ms.auth.core.authentication.loginevent.service.iservice.IAuthLoginEventService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Writes the authentication outcome audit.
 *
 * **Why the identifier is only ever a digest.** Failed attempts for accounts that do not exist are exactly the
 * rows an operator needs, and they are also the rows an attacker would most like to read: a plaintext column
 * would accumulate into a list of names worth trying elsewhere. The digest still supports the questions the
 * audit is for — "how many attempts against this same name", "is this the account somebody keeps guessing" —
 * because the service hashes the search term the same way.
 */
@Service
open class AuthLoginEventService(
    private val dao: AuthLoginEventDao,
) : IAuthLoginEventService {
    private val log = LogFactory.getLog(this::class)

    /**
     * Its own transaction, and deliberately swallowing failures.
     *
     * The authentication decision has already been made and persisted by the time this runs. Joining the
     * caller's transaction would let an audit failure roll back a completed sign-in; rethrowing would let an
     * audit outage deny service to everyone. Neither is a safer answer than a logged error and a metric the
     * deployment can alarm on.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun record(command: AuthLoginEventRecordCommand): Boolean = try {
        recordOrThrow(command)
    } catch (e: Exception) {
        log.error(
            e,
            "Failed to record authentication outcome for transaction ${command.transactionId}; " +
                "the authentication result itself is unaffected",
        )
        false
    }

    override fun identifierHash(identifier: String): String = digest(normalize(identifier))

    @Transactional(readOnly = true)
    override fun listRecent(
        tenantId: String,
        userId: String?,
        identifier: String?,
        successOnly: Boolean?,
        limit: Int,
    ): List<AuthLoginEvent> {
        requireIdentifierValue(tenantId, TENANT_INVALID)
        userId?.let { requireIdentifierValue(it, USER_INVALID) }
        if (limit !in 1..MAX_QUERY_LIMIT) fail(LIMIT_INVALID)
        val hash = identifier?.trim()?.takeIf { it.isNotEmpty() }?.let(::identifierHash)
        return dao.findRecent(tenantId, userId, hash, successOnly, limit).map { it.toEvent() }
    }

    private fun recordOrThrow(command: AuthLoginEventRecordCommand): Boolean {
        requireIdentifierValue(command.transactionId, TRANSACTION_INVALID)
        // The table's unique key would refuse a duplicate anyway; checking first keeps a benign replay from
        // showing up as an error in the log every time.
        if (dao.findByTransaction(command.transactionId) != null) return false
        if (command.success && command.userId == null) fail(SUCCESS_WITHOUT_USER)
        if (!command.success && command.failureCode.isNullOrBlank()) fail(FAILURE_WITHOUT_CODE)
        val event = AuthLoginEventPo {
            id = UUID.randomUUID().toString()
            tenantId = command.tenantId?.trimToNull(MAX_ID_LENGTH)
            userId = command.userId?.trimToNull(MAX_ID_LENGTH)
            identifierHash = command.identifier?.trim()?.takeIf { it.isNotEmpty() }?.let { digest(normalize(it)) }
            providerId = command.providerId?.trimToNull(MAX_ID_LENGTH)
            authenticationMethod = command.authenticationMethod?.trimToNull(MAX_METHOD_LENGTH)
            transactionId = command.transactionId
            purpose = command.purpose.name
            sessionId = command.sessionId?.trimToNull(MAX_SESSION_LENGTH)
            success = command.success
            failureCode = command.failureCode?.trimToNull(MAX_CODE_LENGTH)
            acr = command.acr?.trimToNull(MAX_CODE_LENGTH)
            amr = command.amr.filter { it.isNotBlank() }.sorted().joinToString(",").takeIf { it.isNotEmpty() }
                ?.take(MAX_AMR_LENGTH)
            loginIp = command.observation.loginIp
            loginLocation = command.observation.loginLocation?.trimToNull(MAX_SHORT_TEXT)
            loginDevice = command.observation.loginDevice?.trimToNull(MAX_NAME_LENGTH)
            loginBrowser = command.observation.loginBrowser?.trimToNull(MAX_NAME_LENGTH)
            loginOs = command.observation.loginOs?.trimToNull(MAX_NAME_LENGTH)
            userAgent = command.observation.userAgent?.trimToNull(MAX_USER_AGENT_LENGTH)
            occurredAt = command.occurredAt
        }
        dao.insert(event)
        return true
    }

    private fun AuthLoginEventPo.toEvent() = AuthLoginEvent(
        id = id,
        tenantId = tenantId,
        userId = userId,
        identifierHash = identifierHash,
        providerId = providerId,
        authenticationMethod = authenticationMethod,
        transactionId = transactionId,
        purpose = runCatching { AuthenticationTransactionPurposeEnum.valueOf(purpose) }
            .getOrElse { fail(PURPOSE_INVALID) },
        sessionId = sessionId,
        success = success,
        failureCode = failureCode,
        acr = acr,
        amr = amr?.split(',')?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet(),
        loginIp = loginIp,
        loginLocation = loginLocation,
        loginDevice = loginDevice,
        loginBrowser = loginBrowser,
        loginOs = loginOs,
        userAgent = userAgent,
        occurredAt = occurredAt,
    )

    /** Case- and whitespace-insensitive, so the same attempted name digests the same way every time. */
    private fun normalize(identifier: String): String = identifier.trim().lowercase(Locale.ROOT)

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    /** Truncates rather than refuses: an over-long user agent is not a reason to lose the audit row. */
    private fun String.trimToNull(maxLength: Int): String? =
        trim().takeIf { it.isNotEmpty() }?.take(maxLength)

    private fun requireIdentifierValue(value: String, errorCode: String) {
        if (value.isBlank() || value.length > MAX_ID_LENGTH || value.any(Char::isISOControl)) fail(errorCode)
    }

    private fun fail(errorCode: String): Nothing = throw AuthLoginEventException(errorCode)

    internal companion object {
        const val MAX_QUERY_LIMIT = 500
        const val MAX_ID_LENGTH = 36
        const val MAX_METHOD_LENGTH = 64
        const val MAX_SESSION_LENGTH = 64
        const val MAX_CODE_LENGTH = 64
        const val MAX_AMR_LENGTH = 128
        const val MAX_NAME_LENGTH = 64
        const val MAX_SHORT_TEXT = 128
        const val MAX_USER_AGENT_LENGTH = 512
        const val TENANT_INVALID = "AUTH_LOGIN_EVENT_TENANT_INVALID"
        const val USER_INVALID = "AUTH_LOGIN_EVENT_USER_INVALID"
        const val LIMIT_INVALID = "AUTH_LOGIN_EVENT_LIMIT_INVALID"
        const val TRANSACTION_INVALID = "AUTH_LOGIN_EVENT_TRANSACTION_INVALID"
        const val SUCCESS_WITHOUT_USER = "AUTH_LOGIN_EVENT_SUCCESS_WITHOUT_USER"
        const val FAILURE_WITHOUT_CODE = "AUTH_LOGIN_EVENT_FAILURE_WITHOUT_CODE"
        const val PURPOSE_INVALID = "AUTH_LOGIN_EVENT_PURPOSE_INVALID"
    }
}
