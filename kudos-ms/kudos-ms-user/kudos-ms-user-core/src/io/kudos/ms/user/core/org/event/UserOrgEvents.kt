package io.kudos.ms.user.core.org.event

/**
 * Organization (`user_org`) domain events. Dispatched by `@TransactionalEventListener(AFTER_COMMIT)`.
 *
 * Key point: update / delete events carry a "pre-event snapshot" of parentId, so downstream consumers
 * that need to invalidate caches by ancestor chain (e.g. [io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache])
 * can still precisely know "which ancestors to clear" after the transaction is committed. At AFTER_COMMIT,
 * the old parentId in the database has already been overwritten / the row has been deleted, so the listener
 * itself cannot query it back.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserOrgEvent {
    val id: String
}

data class UserOrgInserted(override val id: String) : UserOrgEvent

/**
 * Covers general update, updateActive, moveOrg and other partial field updates.
 *
 * `oldParentId` / `newParentId` are obtained by the service via dual SELECT before publish (before + after).
 * - Non-move updates (updateActive / name change, etc.): the two are equal.
 * - Move updates (moveOrg): the two differ; the listener should evict the old chain and the new chain separately.
 * - Default value null retains source compatibility with old publishers that do not pass the snapshot; new code should always pass it.
 */
data class UserOrgUpdated(
    override val id: String,
    val oldParentId: String? = null,
    val newParentId: String? = null,
) : UserOrgEvent

/**
 * Deletes a single organization. `parentId` is a pre-deletion snapshot (after deletion dao.get(id) is null).
 */
data class UserOrgDeleted(
    override val id: String,
    val parentId: String? = null,
) : UserOrgEvent

/**
 * Batch deletes organizations. `items` is the (id, parentId) snapshot of each deleted organization.
 * `ids` is retained as a computed property; the old listener's `event.ids` is unchanged.
 */
data class UserOrgBatchDeleted(val items: Collection<Item>) : UserOrgEvent {
    data class Item(val id: String, val parentId: String?)

    override val id: String get() = items.first().id

    /** For compatibility with downstream listeners that invalidate caches by id only. */
    val ids: Collection<String> get() = items.map { it.id }
}
