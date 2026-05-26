package io.kudos.ms.auth.core.group.event

/**
 * Group (`auth_group`) domain events. Dispatched via `@TransactionalEventListener(AFTER_COMMIT)`.
 *
 * Snapshot pattern for delete events: the service reads `tenantId`/`code` before `super.deleteById`/
 * `batchDelete`, then carries them on the event so downstream caches indexed by (tenantId, code) can
 * invalidate precisely (the DB row no longer exists at AFTER_COMMIT time).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthGroupEvent {
    val id: String
}

data class AuthGroupInserted(override val id: String) : AuthGroupEvent

/** Covers generic update, updateActive, and other partial-field updates. */
data class AuthGroupUpdated(override val id: String) : AuthGroupEvent

data class AuthGroupDeleted(
    override val id: String,
    val tenantId: String,
    val code: String,
) : AuthGroupEvent

data class AuthGroupBatchDeleted(val items: Collection<Item>) : AuthGroupEvent {
    data class Item(val id: String, val tenantId: String, val code: String)

    override val id: String get() = items.first().id

    /** Convenience for downstream listeners that invalidate caches by id only. */
    val ids: Collection<String> get() = items.map { it.id }
}
