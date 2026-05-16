package io.kudos.ms.user.core.account.event

/**
 * 用户账号（`user_account`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发，
 * 与 sys 模块同套路（见 ms-sys 中 accessrule / tenant / dict 等域的 PoC）。
 *
 * 删除类事件 snapshot 模式：服务层在 `super.deleteById`/`batchDelete` 前先读取
 * `tenantId`/`username`，再随事件投递，方便按 (tenantId, username) 索引的下游缓存做精确失效
 * （AFTER_COMMIT 时数据库已无行可查）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserAccountEvent {
    val id: String
}

data class UserAccountInserted(override val id: String) : UserAccountEvent

/** 涵盖一般 update、updateActive、各类部分字段更新（密码、登录错误次数、登录登出时间等）。 */
data class UserAccountUpdated(override val id: String) : UserAccountEvent

data class UserAccountDeleted(
    override val id: String,
    val tenantId: String,
    val username: String,
) : UserAccountEvent

data class UserAccountBatchDeleted(val items: Collection<Item>) : UserAccountEvent {
    data class Item(val id: String, val tenantId: String, val username: String)

    override val id: String get() = items.first().id

    /** 兼容仅按 id 失效缓存的下游 listener。 */
    val ids: Collection<String> get() = items.map { it.id }
}
