package io.kudos.ms.sys.core.domain.event

/**
 * Domain (`sys_domain`) domain events.
 *
 * The domain cache uses `domain name` as its key, so the delete event must carry the domain string —
 * it cannot be looked up after the DB row is deleted.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysDomainEvent {
    val id: String
}

data class SysDomainInserted(override val id: String) : SysDomainEvent
data class SysDomainUpdated(override val id: String) : SysDomainEvent

/** Delete event carries the domain string so the cache can be invalidated by name key. */
data class SysDomainDeleted(
    override val id: String,
    val domain: String,
) : SysDomainEvent

/** Batch delete: carries the set of domain strings for each deleted record. */
data class SysDomainBatchDeleted(
    val ids: Collection<String>,
    val domains: Set<String>,
) : SysDomainEvent {
    override val id: String get() = ids.first()
}
