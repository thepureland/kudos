package io.kudos.ms.user.core.org.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserAdminUpdated
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 用户ID列表（by org）缓存处理器。
 *
 * **语义为"该机构及其所有启用子孙机构下的用户集合"**：
 * - 数据来源：`user_org_user`（直接挂载）+ `user_org.parent_id`（递归展开子机构）
 * - 缓存的 value 是 self + 所有 active 子机构的用户 ID 列表（去重）
 *
 * 这样做的业务原因：父机构看子机构成员是常见需求（如"销售总监查看销售部全员"）。
 * 如果调用方只要直接成员，应直接走 [UserOrgUserDao.searchUserIdsByOrgId]，不要用本缓存。
 *
 * 缓存 key：orgId；value：List<String>。
 *
 * **缓存失效**：
 * - 关系变更 ([UserOrgUserRelationsChanged] / [UserOrgUserAdminUpdated]) 影响 orgId 及其
 *   所有祖先的视图，因此监听器会沿 parent_id 向上失效一整条链。
 * - 机构树自身变更：
 *   - [UserOrgUpdated] 携带 oldParentId / newParentId 快照。非移动类（active 改、name 改等）
 *     两者相等，沿单条祖先链失效。移动类（moveOrg）两者不等，分别失效旧链 + 新链。
 *   - [UserOrgDeleted] / [UserOrgBatchDeleted] 携带删除前的 parentId 快照，沿祖先链失效；
 *     同时失效 orgId 自身（哪怕已删除，缓存条目可能还在）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByOrgIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var userOrgUserDao: UserOrgUserDao

    @Autowired
    private lateinit var userOrgDao: UserOrgDao

    companion object {
        private const val CACHE_NAME = "USER_IDS_BY_ORG_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByOrgIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有机构下的用户ID！")
            return
        }

        val orgIdToParentId = userOrgDao.searchAllOrgIdToParentId()
        val orgIdToDirectUserIds = userOrgUserDao.searchAllOrgIdToUserIds()
        // 反向构造 parentId → [childId] 索引；只保留 parent != null 的（根机构不入索引但仍要 evaluate）
        val parentToChildren: Map<String, List<String>> = orgIdToParentId.entries
            .mapNotNull { (childId, parentId) -> parentId?.let { it to childId } }
            .groupBy({ it.first }, { it.second })
        log.debug(
            "加载${orgIdToParentId.size}个机构、${orgIdToDirectUserIds.size}个机构-用户分组，构造父子索引 ${parentToChildren.size} 条。"
        )

        if (clear) {
            clear()
        }

        orgIdToParentId.keys.forEach { rootOrgId ->
            val included = mutableSetOf(rootOrgId)
            val queue = ArrayDeque<String>().apply { add(rootOrgId) }
            while (queue.isNotEmpty()) {
                parentToChildren[queue.removeFirst()]?.forEach { childId ->
                    if (included.add(childId)) queue.add(childId)
                }
            }
            val userIds = included.flatMap { orgIdToDirectUserIds[it].orEmpty() }.distinct()
            if (userIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, rootOrgId, userIds)
                log.debug("缓存了机构${rootOrgId}（含${included.size}个子树节点）的${userIds.size}条用户ID。")
            }
        }
    }

    /**
     * 根据机构ID获取其下所有用户ID（含递归子机构成员，去重）。
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#orgId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(orgId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在机构${orgId}的用户ID，从数据库中加载...")
        }
        val orgIds = userOrgDao.searchOrgAndDescendantIds(orgId)
        val userIds = userOrgUserDao.searchUserIdsByOrgIds(orgIds).distinct()
        log.debug("从数据库加载了机构${orgId}及子机构（共${orgIds.size}个）的${userIds.size}条用户ID。")
        return userIds
    }

    /** @deprecated 保留入口做向后兼容；新代码走事件机制。 */
    open fun syncOnInsert(any: Any, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("新增id为${id}的机构-用户关系后，同步${CACHE_NAME}缓存...")
        val orgId = BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
        syncByOrgAndAncestors(orgId)
    }

    /** @deprecated 保留入口做向后兼容。 */
    open fun syncOnUpdate(any: Any?, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("更新id为${id}的机构-用户关系后，同步${CACHE_NAME}缓存...")
        val orgId = if (any == null) {
            requireNotNull(userOrgUserDao.get(id)) { "更新机构-用户关系缓存时找不到id=$id 的记录。" }.orgId
        } else {
            BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
        }
        syncByOrgAndAncestors(orgId)
    }

    /** @deprecated 保留入口做向后兼容。 */
    open fun syncOnDelete(any: Any, id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val orgId = BeanKit.getProperty(any, UserOrgUser::orgId.name) as String
        log.debug("删除id为${id}的机构-用户关系后，同步从${CACHE_NAME}缓存中踢除...")
        syncByOrgAndAncestors(orgId)
    }

    /** @deprecated 保留入口做向后兼容。 */
    open fun syncOnBatchDelete(ids: Collection<String>, orgIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("批量删除id为${ids}的机构-用户关系后，同步从${CACHE_NAME}缓存中踢除...")
        orgIds.toSet().forEach { syncByOrgAndAncestors(it) }
    }

    /**
     * @deprecated 保留旧公开入口做向后兼容；新代码请走事件机制 [UserOrgUserRelationsChanged]。
     *
     * 与之前不同：该方法现在会沿 parent_id 上溯，把 orgId 自身 + 所有祖先机构的缓存条目
     * 一并失效。原因：祖先机构的 user 视图含 orgId 的成员，关系变更需要在父链上都重算。
     */
    open fun syncOnOrgUserChange(orgId: String) = syncByOrgAndAncestors(orgId)

    /**
     * 内部统一失效路径：失效 orgId 及其所有祖先的缓存。
     */
    private fun syncByOrgAndAncestors(orgId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val chain = userOrgDao.searchOrgAndAncestorIds(orgId)
        chain.forEach { id ->
            KeyValueCacheKit.evict(CACHE_NAME, id)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByOrgIdCache>().getUserIds(id)
            }
        }
        log.debug("${CACHE_NAME}缓存同步完成，沿祖先链失效了${chain.size}个机构。")
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUserRelationsChanged): Unit = syncByOrgAndAncestors(event.orgId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUserAdminUpdated): Unit = syncByOrgAndAncestors(event.orgId)

    /**
     * 机构本身被改动（active 切换 / moveOrg / 通用 update）。失效策略：
     * - 自己：缓存条目可能含子树聚合，必须重算 → evict
     * - oldParentId 祖先链：移动后旧链的"含子树成员"视图不再含本 org，evict
     * - newParentId 祖先链：移动后新链多了本 org 子树，evict
     *   非移动类 update 时 oldParentId == newParentId，两条链合一，evict 一次
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgUpdated) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        evictAndReload(event.id)
        // 旧的没传时 fallback 走 now-DB-lookup（旧 publisher 不会这么做，但稳健）。null 表示已是根。
        listOfNotNull(event.oldParentId, event.newParentId).distinct().forEach { syncByOrgAndAncestors(it) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        // 删了的 org 本身的缓存条目也要清；它的祖先链失去这棵子树成员
        evictAndReload(event.id)
        event.parentId?.let { syncByOrgAndAncestors(it) }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserOrgBatchDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        // 每条单独失效自己 + 祖先链。祖先链 IDs 可能重复 —— 用 set 去重少几次 DB walk。
        event.items.forEach { evictAndReload(it.id) }
        val parentIds = event.items.mapNotNullTo(mutableSetOf()) { it.parentId }
        parentIds.forEach { syncByOrgAndAncestors(it) }
    }

    /** 单一 key 的 evict + 按需 write-in-time 重算。给 org 自身条目使用（不走父链）。 */
    private fun evictAndReload(orgId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, orgId)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            // 重算可能在 org 已被删除的场景下查不到 → 返回空，@Cacheable unless=isEmpty 不会写回，OK
            getSelf<UserIdsByOrgIdCache>().getUserIds(orgId)
        }
    }

    private val log = LogFactory.getLog(this::class)

}
