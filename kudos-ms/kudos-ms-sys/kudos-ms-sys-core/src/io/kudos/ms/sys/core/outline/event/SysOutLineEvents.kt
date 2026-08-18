package io.kudos.ms.sys.core.outline.event

/**
 * Outbound allowlist (`sys_out_line`) domain events.
 *
 * The delete event carries `systemCode` + `tenantId` so cache entries can be precisely invalidated by dimension; after the DB delete it can no longer be looked up.
 *
 * @author K
 * @since 1.0.0
 */
sealed interface SysOutLineEvent {
    val id: String
}

data class SysOutLineInserted(override val id: String) : SysOutLineEvent
data class SysOutLineUpdated(override val id: String) : SysOutLineEvent

data class SysOutLineDeleted(
    override val id: String,
    val systemCode: String,
    val tenantId: String?,
) : SysOutLineEvent

/** Batch delete: carries the dimension set of each record for precise invalidation. */
data class SysOutLineBatchDeleted(
    val ids: Collection<String>,
    val dimensions: Set<Pair<String, String?>>,
) : SysOutLineEvent {
    override val id: String get() = ids.first()
}
