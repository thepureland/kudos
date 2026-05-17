package io.kudos.ms.sys.core.domain.event

/**
 * 域名（`sys_domain`）领域事件。
 *
 * 域名缓存以 `domain name` 作为 key，所以删除事件需要携带 domain 字符串——
 * DB 删除后无法回查。
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

/** 删除事件携带 domain 字符串，便于按 name key 失效缓存。 */
data class SysDomainDeleted(
    override val id: String,
    val domain: String,
) : SysDomainEvent

/** 批量删除：携带每条记录的 domain 集合。 */
data class SysDomainBatchDeleted(
    val ids: Collection<String>,
    val domains: Set<String>,
) : SysDomainEvent {
    override val id: String get() = ids.first()
}
