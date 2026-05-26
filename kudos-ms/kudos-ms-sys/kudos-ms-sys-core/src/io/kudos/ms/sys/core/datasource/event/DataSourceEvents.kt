package io.kudos.ms.sys.core.datasource.event

/**
 * Data source (`sys_data_source`) domain events.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysDataSourceEvent {
    val id: String
}

data class SysDataSourceInserted(override val id: String) : SysDataSourceEvent
data class SysDataSourceUpdated(override val id: String) : SysDataSourceEvent
data class SysDataSourceDeleted(override val id: String) : SysDataSourceEvent
data class SysDataSourceBatchDeleted(val ids: Collection<String>) : SysDataSourceEvent {
    override val id: String get() = ids.first()
}
