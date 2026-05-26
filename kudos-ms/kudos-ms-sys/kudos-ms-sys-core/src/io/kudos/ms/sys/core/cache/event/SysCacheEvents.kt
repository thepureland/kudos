package io.kudos.ms.sys.core.cache.event

/**
 * Cache configuration (`sys_cache`) domain events.
 *
 * **Uses a plain `@EventListener` (not `@TransactionalEventListener`)**: this domain's service tests
 * assert that the new cache state is immediately readable after `service.mutation(...)`. In Spring
 * `@Transactional` auto-rollback unit tests, AFTER_COMMIT does not fire, so this domain uses synchronous
 * events to preserve the old direct-sync semantics while removing the service's direct dependency on the cache implementation.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysCacheEvent {
    val id: String
}

data class SysCacheInserted(override val id: String) : SysCacheEvent

/** Covers general update and updateActive operations. */
data class SysCacheUpdated(override val id: String) : SysCacheEvent

data class SysCacheDeleted(override val id: String) : SysCacheEvent

data class SysCacheBatchDeleted(val ids: Collection<String>) : SysCacheEvent {
    override val id: String get() = ids.first()
}
