package io.kudos.ms.user.core.org.event

/**
 * 机构（`user_org`）领域事件。由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * 关键：在 update / delete 类事件中携带 parentId 的"事前快照"，让需要按祖先链
 * 失效缓存的下游（如 [io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache]）
 * 能在事务提交之后仍精确知道"应该清哪些祖先"。AFTER_COMMIT 时数据库的旧 parentId
 * 已经被覆盖 / 行已删除，listener 自己查不回去。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface UserOrgEvent {
    val id: String
}

data class UserOrgInserted(override val id: String) : UserOrgEvent

/**
 * 涵盖一般 update、updateActive、moveOrg 等部分字段更新。
 *
 * `oldParentId` / `newParentId` 在 publish 前由 service 双 SELECT 得到（事前 + 事后）。
 * - 非移动类更新（updateActive / 改 name 等）：两者相等。
 * - 移动类更新（moveOrg）：两者不等，listener 应分别 evict 旧链和新链。
 * - 默认值 null 保留对未传 snapshot 的旧 publisher 的源码兼容；新代码应总是传。
 */
data class UserOrgUpdated(
    override val id: String,
    val oldParentId: String? = null,
    val newParentId: String? = null,
) : UserOrgEvent

/**
 * 删除单个机构。`parentId` 是删除前的快照（删除后 dao.get(id) 是 null）。
 */
data class UserOrgDeleted(
    override val id: String,
    val parentId: String? = null,
) : UserOrgEvent

/**
 * 批量删除机构。`items` 是每个被删除机构的 (id, parentId) 快照。
 * `ids` 作为计算属性保留，旧 listener `event.ids` 不变。
 */
data class UserOrgBatchDeleted(val items: Collection<Item>) : UserOrgEvent {
    data class Item(val id: String, val parentId: String?)

    override val id: String get() = items.first().id

    /** 兼容仅按 id 失效缓存的下游 listener。 */
    val ids: Collection<String> get() = items.map { it.id }
}
