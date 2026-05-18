package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 角色ID列表（by user id）缓存处理器。
 *
 * **语义为"有效角色集合"**：
 * - 数据来源表：`auth_role_user`（直接授权）∪ `auth_group_user` + `auth_group_role`（通过组继承）
 * - 缓存的 value 是这两条路径 union + 去重后的角色 ID 列表
 *
 * 之所以把"组继承"也算进来，是因为本 cache 的下游调用方（[io.kudos.ms.auth.core.role.service.impl.AuthRoleService.hasRole]
 * / [getUserRoles] / [getUserRoleIds] / 资源缓存）问的都是"该用户在权限判定时拥有哪些角色"，而不是"直接绑定了哪些角色"。
 * 需要后者的调用方应直接走 [io.kudos.ms.auth.core.role.dao.AuthRoleUserDao.searchRoleIdsByUserId]。
 *
 * 缓存 key：userId；value：List<String>。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class RoleIdsByUserIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "AUTH_ROLE_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<RoleIdsByUserIdCache>().getRoleIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的角色ID！")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToDirectRoleIds = authRoleUserDao.searchAllUserIdToRoleIdsForCache()
        val userIdToGroupIds = authGroupUserDao.searchAllUserIdToGroupIdsForCache()
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()

        log.debug(
            "从数据库加载了${users.size}条用户、直接角色分组${userIdToDirectRoleIds.size}、" +
                "用户-组分组${userIdToGroupIds.size}、组-角色分组${groupIdToRoleIds.size}。"
        )

        if (clear) {
            clear()
        }

        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val effectiveRoleIds = computeEffectiveRoleIds(
                directRoleIds = userIdToDirectRoleIds[userId].orEmpty(),
                groupIds = userIdToGroupIds[userId].orEmpty(),
                groupIdToRoleIds = groupIdToRoleIds,
            )
            if (effectiveRoleIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, userId, effectiveRoleIds)
                log.debug("缓存了用户${userId}的${effectiveRoleIds.size}条有效角色ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户的有效角色 ID 列表，缓存不存在时从数据库加载并回写。
     * 有效 = 直接授权 ∪ 通过用户组继承
     *
     * @param userId 用户ID
     * @return List<角色ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getRoleIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的角色ID，从数据库中加载...")
        }
        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val groupDerived = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        val effective = (direct + groupDerived).distinct()
        log.debug(
            "从数据库加载了用户${userId}的有效角色：直接${direct.size}，组继承${groupDerived.size}，去重${effective.size}。"
        )
        return effective
    }

    private fun computeEffectiveRoleIds(
        directRoleIds: Collection<String>,
        groupIds: Collection<String>,
        groupIdToRoleIds: Map<String, List<String>>,
    ): List<String> {
        if (directRoleIds.isEmpty() && groupIds.isEmpty()) return emptyList()
        val groupDerived = groupIds.flatMap { groupIdToRoleIds[it].orEmpty() }
        return (directRoleIds + groupDerived).distinct()
    }

    /**
     * @deprecated 保留单用户公开入口做向后兼容；新代码请走事件机制（[AuthRoleUserRelationsChanged] 等）。
     */
    open fun syncOnRoleUserChange(userId: String): Unit = syncByUserIds(listOf(userId))

    /**
     * @deprecated 保留批量公开入口做向后兼容；新代码请走事件机制。
     */
    open fun syncOnBatchRoleUserChange(userIds: Collection<String>): Unit = syncByUserIds(userIds)

    /**
     * 用户-角色 / 用户-组 / 组-角色 关系变更后同步缓存：直接按 userIds 失效即可。
     */
    private fun syncByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        userIds.forEach { userId ->
            KeyValueCacheKit.evict(CACHE_NAME, userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<RoleIdsByUserIdCache>().getRoleIds(userId)
            }
        }
        log.debug("${CACHE_NAME}缓存同步完成，共影响${userIds.size}个用户。")
    }

    /** 用户删除后清掉该 userId 下的 roleId 列表。 */
    private fun evictByUserId(userId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /** 用户被加进 / 移出一个组，等价于直接角色集合变化。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /**
     * 组关联的角色变了 → 该组下所有用户的有效角色集合都要重算。
     * 把 groupId 展开为 userIds 后走统一失效路径。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val userIds = authGroupUserDao.searchUserIdsByGroupId(event.groupId)
        if (userIds.isEmpty()) return
        syncByUserIds(userIds)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictByUserId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.ids.forEach(::evictByUserId)
    }

    private val log = LogFactory.getLog(this::class)

}
