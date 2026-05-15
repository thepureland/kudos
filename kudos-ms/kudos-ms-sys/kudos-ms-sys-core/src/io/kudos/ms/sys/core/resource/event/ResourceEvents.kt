package io.kudos.ms.sys.core.resource.event

/**
 * 资源（`sys_resource`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * Hash 缓存的副属性索引由框架自动维护（按 FILTERABLE_PROPERTIES 在保存 / 删除时同步索引），
 * 所以事件无需携带 dim 信息——仅 id 即可。
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
