package io.kudos.ms.sys.core.tenant.event

/**
 * Tenant (`sys_tenant`) domain events. Dispatched after transaction commit via
 * [Spring `@TransactionalEventListener(AFTER_COMMIT)`][org.springframework.transaction.event.TransactionalEventListener]
 * to avoid polluting the cache when transactions roll back.
 *
 * Design aligned with the equivalent events in the access rule domain (see P1 PoC); subscribers look up the latest
 * row or evict the cache by the id carried in the event, instead of having the service call `cache.syncOnX(...)` directly.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysTenantEvent {
    val id: String
}

/** Insert completion event. */
data class SysTenantInserted(override val id: String) : SysTenantEvent

/** Update completion event (including `updateActive` and general updates). */
data class SysTenantUpdated(override val id: String) : SysTenantEvent

/** Delete completion event. */
data class SysTenantDeleted(override val id: String) : SysTenantEvent

/** Batch delete completion event. */
data class SysTenantBatchDeleted(val ids: Collection<String>) : SysTenantEvent {
    override val id: String get() = ids.first()
}
