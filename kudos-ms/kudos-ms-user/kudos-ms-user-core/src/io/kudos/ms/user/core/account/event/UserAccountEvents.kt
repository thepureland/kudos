package io.kudos.ms.user.core.account.event

/**
 * 用户账号（`user_account`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发，
 * 与 sys 模块同套路（见 ms-sys 中 accessrule / tenant / dict 等域的 PoC）。
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

data class UserAccountDeleted(override val id: String) : UserAccountEvent
data class UserAccountBatchDeleted(val ids: Collection<String>) : UserAccountEvent {
    override val id: String get() = ids.first()
}
