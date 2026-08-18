package io.kudos.ms.sys.core.dict.event

/**
 * Domain events for dictionary items (`sys_dict_item`). Dispatched by `@TransactionalEventListener(AFTER_COMMIT)`.
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
 * Deletion-completed event. Carries auxiliary attribute dimensions (atomicServiceCode, dictType, itemCode) so the
 * Hash cache can clean up the corresponding secondary indexes when removing the primary key—this information can no
 * longer be looked up once the DB deletion has occurred.
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
