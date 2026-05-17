package io.kudos.ms.sys.core.system.event

/**
 * 系统（`sys_system`）领域事件。`id` 即 code（PK 为业务编码）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysSystemEvent {
    val id: String
}

data class SysSystemInserted(override val id: String) : SysSystemEvent
data class SysSystemUpdated(override val id: String) : SysSystemEvent
data class SysSystemDeleted(override val id: String) : SysSystemEvent
data class SysSystemBatchDeleted(val ids: Collection<String>) : SysSystemEvent {
    override val id: String get() = ids.first()
}
