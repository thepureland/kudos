package io.kudos.ms.sys.core.outline.event

/**
 * 出网白名单（`sys_out_line`）领域事件
 *
 * 删除事件携带 `systemCode` + `tenantId`，便于按缓存维度精确失效；DB 删除后已无法回查。
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

/** 批量删除：携带每条记录的维度集合用于精确失效。 */
data class SysOutLineBatchDeleted(
    val ids: Collection<String>,
    val dimensions: Set<Pair<String, String?>>,
) : SysOutLineEvent {
    override val id: String get() = ids.first()
}
