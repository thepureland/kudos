package io.kudos.ms.sys.core.dict.event

/**
 * 字典（`sys_dict`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发，
 * 与 access rule / tenant 域同套路。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysDictEvent {
    val id: String
}

data class SysDictInserted(override val id: String) : SysDictEvent
data class SysDictUpdated(override val id: String) : SysDictEvent
data class SysDictDeleted(override val id: String) : SysDictEvent
data class SysDictBatchDeleted(val ids: Collection<String>) : SysDictEvent {
    override val id: String get() = ids.first()
}
