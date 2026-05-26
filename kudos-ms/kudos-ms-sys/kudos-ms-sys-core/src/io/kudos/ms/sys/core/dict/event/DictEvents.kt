package io.kudos.ms.sys.core.dict.event

/**
 * Domain events for dictionaries (`sys_dict`). Dispatched by `@TransactionalEventListener(AFTER_COMMIT)`,
 * following the same pattern as the access rule / tenant domains.
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
