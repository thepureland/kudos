package io.kudos.ms.auth.core.role.event

/**
 * Role (`auth_role`) domain events. Dispatched via `@TransactionalEventListener(AFTER_COMMIT)`.
 *
 * Snapshot pattern for delete events: the service reads `tenantId`/`code` before `super.deleteById`/
 * `batchDelete`, then carries them on the event so downstream caches indexed by (tenantId, code) can
 * invalidate precisely (the DB row no longer exists at AFTER_COMMIT time).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthRoleEvent {
    val id: String
}

data class AuthRoleInserted(override val id: String) : AuthRoleEvent

/** Covers generic update, updateActive, and other partial-field updates. */
data class AuthRoleUpdated(override val id: String) : AuthRoleEvent

data class AuthRoleDeleted(
    override val id: String,
    val tenantId: String,
    val code: String,
) : AuthRoleEvent

data class AuthRoleBatchDeleted(val items: Collection<Item>) : AuthRoleEvent {
    data class Item(val id: String, val tenantId: String, val code: String)

    override val id: String get() = items.first().id

    /** Convenience for downstream listeners that invalidate caches by id only. */
    val ids: Collection<String> get() = items.map { it.id }
}
