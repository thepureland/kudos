package io.kudos.ms.auth.core.role.event

/**
 * 角色（`auth_role`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * 删除类事件 snapshot 模式：服务层在 `super.deleteById`/`batchDelete` 前先读取
 * `tenantId`/`code`，再随事件投递，方便按 (tenantId, code) 索引的下游缓存做精确失效
 * （AFTER_COMMIT 时数据库已无行可查）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface AuthRoleEvent {
    val id: String
}

data class AuthRoleInserted(override val id: String) : AuthRoleEvent

/** 涵盖一般 update、updateActive 等部分字段更新。 */
data class AuthRoleUpdated(override val id: String) : AuthRoleEvent

data class AuthRoleDeleted(
    override val id: String,
    val tenantId: String,
    val code: String,
) : AuthRoleEvent

data class AuthRoleBatchDeleted(val items: Collection<Item>) : AuthRoleEvent {
    data class Item(val id: String, val tenantId: String, val code: String)

    override val id: String get() = items.first().id

    /** 兼容仅按 id 失效缓存的下游 listener。 */
    val ids: Collection<String> get() = items.map { it.id }
}
