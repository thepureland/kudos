package io.kudos.ms.user.core.org.event

/**
 * 机构（`user_org`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserOrgEvent {
    val id: String
}

data class UserOrgInserted(override val id: String) : UserOrgEvent

/** 涵盖一般 update、updateActive、moveOrg 等部分字段更新。 */
data class UserOrgUpdated(override val id: String) : UserOrgEvent

data class UserOrgDeleted(override val id: String) : UserOrgEvent

data class UserOrgBatchDeleted(val ids: Collection<String>) : UserOrgEvent {
    override val id: String get() = ids.first()
}
