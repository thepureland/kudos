package io.kudos.ms.auth.core.group.event

/**
 * 用户组（`auth_group`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * 删除类事件 snapshot 模式：服务层在 `super.deleteById`/`batchDelete` 前先读取
 * `tenantId`/`code`，再随事件投递，方便按 (tenantId, code) 索引的下游缓存做精确失效
 * （AFTER_COMMIT 时数据库已无行可查）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthGroupEvent {
    val id: String
}

data class AuthGroupInserted(override val id: String) : AuthGroupEvent

/** 涵盖一般 update、updateActive 等部分字段更新。 */
data class AuthGroupUpdated(override val id: String) : AuthGroupEvent

data class AuthGroupDeleted(
    override val id: String,
    val tenantId: String,
    val code: String,
) : AuthGroupEvent

data class AuthGroupBatchDeleted(val items: Collection<Item>) : AuthGroupEvent {
    data class Item(val id: String, val tenantId: String, val code: String)

    override val id: String get() = items.first().id

    /** 兼容仅按 id 失效缓存的下游 listener。 */
    val ids: Collection<String> get() = items.map { it.id }
}
