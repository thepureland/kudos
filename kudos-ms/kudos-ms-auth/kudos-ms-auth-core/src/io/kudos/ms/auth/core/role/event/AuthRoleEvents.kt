package io.kudos.ms.auth.core.role.event

/**
 * 角色（`auth_role`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
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

data class AuthRoleDeleted(override val id: String) : AuthRoleEvent

data class AuthRoleBatchDeleted(val ids: Collection<String>) : AuthRoleEvent {
    override val id: String get() = ids.first()
}
