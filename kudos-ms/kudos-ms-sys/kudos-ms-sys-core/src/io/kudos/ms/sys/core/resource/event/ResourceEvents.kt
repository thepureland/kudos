package io.kudos.ms.sys.core.resource.event

/**
 * Domain events for resources (`sys_resource`). Dispatched by `@TransactionalEventListener(AFTER_COMMIT)`.
 *
 * Secondary indexes of the Hash cache are maintained automatically by the framework (kept in sync on save / delete
 * by FILTERABLE_PROPERTIES), so events do not need to carry dimension info — only the id is required.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysResourceEvent {
    val id: String
}

data class SysResourceInserted(override val id: String) : SysResourceEvent
data class SysResourceUpdated(override val id: String) : SysResourceEvent
data class SysResourceDeleted(override val id: String) : SysResourceEvent
data class SysResourceBatchDeleted(val ids: Collection<String>) : SysResourceEvent {
    override val id: String get() = ids.first()
}
