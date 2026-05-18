package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 用户资源ID列表（by tenant id & username）缓存处理器。
 *
 * 与 [ResourceIdsByUserIdCache] 完全平行 —— 只是 key 用 (tenantId, username) 而不是 userId。
 * 数据来源：`user_account` ∪ `auth_role_user`（直接角色）∪ `auth_group_user` + `auth_group_role`（组继承角色），
 * 再 join `auth_role_resource` 拿到资源 ID 集合。
 *
 * 缓存 key：tenantId::username；value：List<String>。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByTenantIdAndUsernameCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Resource
    private lateinit var userAccountHashCache: UserAccountHashCache

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    companion object Companion {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_USERNAME"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}用户名"
        }
        val tenantAndUsername = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdsByTenantIdAndUsernameCache>().getResourceIds(
            tenantAndUsername[0], tenantAndUsername[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的资源ID！")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToDirectRoleIds = authRoleUserDao.searchAllUserIdToRoleIdsForCache()
        val userIdToGroupIds = authGroupUserDao.searchAllUserIdToGroupIdsForCache()
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val roleIdToResourceIdsMap = authRoleResourceDao.searchAllRoleIdToResourceIdsForCache()

        log.debug(
            "从数据库加载了${users.size}条用户、直接角色分组${userIdToDirectRoleIds.size}、" +
                "用户-组分组${userIdToGroupIds.size}、组-角色分组${groupIdToRoleIds.size}、" +
                "角色-资源分组${roleIdToResourceIdsMap.size}。"
        )

        if (clear) {
            clear()
        }

        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val tenantId = user.tenantId ?: return@forEach
            val username = user.username ?: return@forEach
            val effectiveRoleIds = computeEffectiveRoleIds(
                directRoleIds = userIdToDirectRoleIds[userId].orEmpty(),
                groupIds = userIdToGroupIds[userId].orEmpty(),
                groupIdToRoleIds = groupIdToRoleIds,
            )
            if (effectiveRoleIds.isEmpty()) return@forEach
            val resourceIds = effectiveRoleIds.flatMap { roleId ->
                roleIdToResourceIdsMap[roleId] ?: emptyList()
            }.distinct()

            if (resourceIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, username), resourceIds)
                log.debug("缓存了租户${tenantId}用户${username}的${resourceIds.size}条资源ID。")
            }
        }
    }

    /**
     * 根据租户ID和用户名从缓存中获取该用户拥有的所有资源ID（直接角色 ∪ 组继承角色 下的所有资源 union 去重）。
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#username)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, username: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}用户${username}的资源ID，从数据库中加载...")
        }

        val userId = userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
        if (userId == null) {
            log.debug("找不到租户${tenantId}的用户${username}。")
            return emptyList()
        }

        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val viaGroup = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        val effectiveRoleIds = (direct + viaGroup).distinct()
        if (effectiveRoleIds.isEmpty()) {
            log.debug("用户${username}没有分配任何（直接或组继承）角色。")
            return emptyList()
        }

        val resultList = authRoleResourceDao.searchResourceIdsByRoleIds(effectiveRoleIds)
        log.debug(
            "从数据库加载了租户${tenantId}用户${username}的${effectiveRoleIds.size}个有效角色" +
                "（直接${direct.size}+组继承${viaGroup.size}），共${resultList.size}条资源ID。"
        )
        return resultList.toList()
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
     * 用户信息更新后同步缓存（用户名或租户变更）。
     */
    open fun syncOnUserUpdate(oldTenantId: String, oldUsername: String, newTenantId: String, newUsername: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("用户信息更新后，同步${CACHE_NAME}缓存...")

        KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldUsername))
        if (oldTenantId != newTenantId || oldUsername != newUsername) {
            KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newUsername))
        }
        log.debug("${CACHE_NAME}缓存同步完成。")
    }

    /** @deprecated 保留旧名做向后兼容；新代码请走事件机制（[AuthGroupUserRelationsChanged] 等）。 */
    open fun syncOnRoleUserChange(tenantId: String, username: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("用户${username}的角色关系变更后，同步${CACHE_NAME}缓存...")
        evict(getKey(tenantId, username))
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<ResourceIdsByTenantIdAndUsernameCache>().getResourceIds(tenantId, username)
        }
    }

    /**
     * 角色-资源关系变更后同步缓存（需要根据角色找到所有关联用户）。
     * 注意：受影响的用户包括 a) 直接拥有该角色的用户 b) 通过组继承该角色的用户。
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("角色${roleId}的资源关系变更后，同步${CACHE_NAME}缓存...")

        val directUserIds = authRoleUserDao.searchUserIdsByRoleId(roleId)
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroupUserIds = groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }.distinct()
        val allUserIds = (directUserIds + viaGroupUserIds).distinct()
        if (allUserIds.isEmpty()) return

        val users = userAccountDao.getByIdsAs<UserAccountCacheEntry>(allUserIds)
        users.forEach { user ->
            val t = user.tenantId ?: return@forEach
            val u = user.username ?: return@forEach
            KeyValueCacheKit.evict(CACHE_NAME, getKey(t, u))
            log.debug("踢除了用户${u}的资源缓存。")
        }
        log.debug(
            "${CACHE_NAME}缓存同步完成，共影响${allUserIds.size}个用户（" +
                "直接${directUserIds.size}+组继承${viaGroupUserIds.size}）。"
        )
    }

    fun getKey(tenantId: String, username: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${username}"
    }

    /** 仅做本地 (tenantId, username) 失效。 */
    private fun evictBy(tenantId: String, username: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, username))
    }

    /** 批量按 userIds 反查 (tenantId, username) 并失效。 */
    private fun evictByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (userIds.isEmpty()) return
        val users = userAccountDao.getByIdsAs<UserAccountCacheEntry>(userIds)
        users.forEach { user ->
            val t = user.tenantId ?: return@forEach
            val u = user.username ?: return@forEach
            KeyValueCacheKit.evict(CACHE_NAME, getKey(t, u))
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted): Unit = evictBy(event.tenantId, event.username)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.username) }
    }

    /** 用户进出组 → 该用户的资源集合变化。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = evictByUserIds(event.userIds)

    /** 组绑定的角色变了 → 组内所有用户的资源集合都要重算。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val userIds = authGroupUserDao.searchUserIdsByGroupId(event.groupId)
        if (userIds.isEmpty()) return
        evictByUserIds(userIds)
    }

    private val log = LogFactory.getLog(this::class)

}
