package io.kudos.ms.sys.core.dict.event

/**
 * 字典项（`sys_dict_item`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysDictItemEvent {
    val id: String
}

data class SysDictItemInserted(override val id: String) : SysDictItemEvent
data class SysDictItemUpdated(override val id: String) : SysDictItemEvent

/**
 * 删除完成事件。携带副属性维度（atomicServiceCode、dictType、itemCode）以便
 * Hash 缓存在删除主键的同时清理对应的二级索引——这些信息在 DB 删除发生后已无法回查。
 */
data class SysDictItemDeleted(
    override val id: String,
    val atomicServiceCode: String,
    val dictType: String?,
    val itemCode: String?,
) : SysDictItemEvent

data class SysDictItemBatchDeleted(val ids: Collection<String>) : SysDictItemEvent {
    override val id: String get() = ids.first()
}
