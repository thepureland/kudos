package io.kudos.ms.user.core.account.event

/**
 * User account (`user_account`) domain events. Dispatched via `@TransactionalEventListener(AFTER_COMMIT)`,
 * following the same pattern as the sys module (see the accessrule / tenant / dict PoCs in ms-sys).
 *
 * Snapshot pattern for delete events: the service reads `tenantId`/`username` before `super.deleteById`/
 * `batchDelete`, then carries them on the event so downstream caches indexed by (tenantId, username) can
 * invalidate precisely (the DB row no longer exists at AFTER_COMMIT time).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserAccountEvent {
    val id: String
}

data class UserAccountInserted(override val id: String) : UserAccountEvent

/** Covers generic update, updateActive, and partial-field updates (password, login error count, login/logout time, etc.). */
data class UserAccountUpdated(override val id: String) : UserAccountEvent

/**
 * Announces a committed account change after which existing authentication state must no longer be trusted.
 *
 * This is deliberately separate from [UserAccountUpdated]: login timestamps and error counters also publish that
 * broad cache-invalidation event, and must never sign a user out.
 */
data class UserAuthenticationInvalidated(
    override val id: String,
    val tenantId: String,
    val reason: Reason,
) : UserAccountEvent {
    enum class Reason {
        LOGIN_PASSWORD_CHANGED,
        SECURITY_PASSWORD_CHANGED,
        ACCOUNT_DISABLED,
        ACCOUNT_FROZEN,
        AUTHENTICATOR_CHANGED,
        WEBAUTHN_CREDENTIAL_CHANGED,
    }
}

data class UserAccountDeleted(
    override val id: String,
    val tenantId: String,
    val username: String,
) : UserAccountEvent

data class UserAccountBatchDeleted(val items: Collection<Item>) : UserAccountEvent {
    data class Item(val id: String, val tenantId: String, val username: String)

    override val id: String get() = items.first().id

    /** Convenience for downstream listeners that invalidate caches by id only. */
    val ids: Collection<String> get() = items.map { it.id }
}
