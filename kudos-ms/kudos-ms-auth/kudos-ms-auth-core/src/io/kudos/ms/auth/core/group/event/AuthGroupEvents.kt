package io.kudos.ms.auth.core.group.event

/**
 * 用户组（`auth_group`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
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

data class AuthGroupDeleted(override val id: String) : AuthGroupEvent

data class AuthGroupBatchDeleted(val ids: Collection<String>) : AuthGroupEvent {
    override val id: String get() = ids.first()
}
