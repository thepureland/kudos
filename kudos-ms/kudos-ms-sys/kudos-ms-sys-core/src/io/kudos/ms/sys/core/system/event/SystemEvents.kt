package io.kudos.ms.sys.core.system.event

/**
 * System (`sys_system`) domain events. `id` is the code (the PK is a business code).
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
