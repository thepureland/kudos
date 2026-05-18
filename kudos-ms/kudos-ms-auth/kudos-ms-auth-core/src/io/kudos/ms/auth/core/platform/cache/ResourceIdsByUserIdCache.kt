package io.kudos.ms.auth.core.platform.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 资源ID列表（by user id）缓存处理器
 *
 * 1.数据来源表：`auth_role_user`（直接角色）∪ `auth_group_user` + `auth_group_role`（组继承角色），
 *   再 join `auth_role_resource` 拿到资源 ID 集合
 * 2.缓存各用户拥有的所有资源ID集合
 * 3.缓存的key为：userId
 * 4.缓存的value为：资源ID集合（List<String>）
 * 5.查询流程：用户 →（直接角色 ∪ 组→角色） → 资源
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByUserIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<ResourceIdsByUserIdCache>().getResourceIds(key)

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
            val effectiveRoleIds = computeEffectiveRoleIds(
                directRoleIds = userIdToDirectRoleIds[userId].orEmpty(),
                groupIds = userIdToGroupIds[userId].orEmpty(),
                groupIdToRoleIds = groupIdToRoleIds,
            )
            if (effectiveRoleIds.isEmpty()) return@forEach
            val resourceIds = effectiveRoleIds.flatMap { roleId ->
                roleIdToResourceIdsMap[roleId] ?: emptyList()
            }.map { it.trim() }.distinct()

            if (resourceIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, userId, resourceIds)
                log.debug("缓存了用户${userId}的${resourceIds.size}条资源ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户拥有的所有资源ID（直接角色 ∪ 组继承角色 下的所有资源 union 去重），
     * 缓存不存在时从数据库加载并回写。
     *
     * @param userId 用户ID
     * @return List<资源ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(userId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的资源ID，从数据库中加载...")
        }

        val direct = authRoleUserDao.searchRoleIdsByUserId(userId)
        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        val groupDerived = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupRoleDao.searchRoleIdsByGroupId(gid) }
        }
        val effectiveRoleIds = (direct + groupDerived).distinct()
        if (effectiveRoleIds.isEmpty()) {
            log.debug("用户${userId}没有分配任何（直接或组继承）角色。")
            return emptyList()
        }

        val resultList = authRoleResourceDao.searchResourceIdsByRoleIds(effectiveRoleIds)
        log.debug(
            "从数据库加载了用户${userId}的${effectiveRoleIds.size}个有效角色（直接${direct.size}+组继承${groupDerived.size}），" +
                "共${resultList.size}条资源ID（去重后）。"
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
     * @deprecated 保留单用户公开入口做向后兼容；新代码请走事件机制（[AuthRoleUserRelationsChanged] 等）。
     */
    open fun syncOnRoleUserChange(userId: String): Unit = syncByUserIds(listOf(userId))

    /**
     * @deprecated 保留批量公开入口做向后兼容；新代码请走事件机制。
     */
    open fun syncOnBatchRoleUserChange(userIds: Collection<String>): Unit = syncByUserIds(userIds)

    /** 用户列表变更后批量失效缓存。 */
    private fun syncByUserIds(userIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        userIds.forEach { userId ->
            KeyValueCacheKit.evict(CACHE_NAME, userId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByUserIdCache>().getResourceIds(userId)
            }
        }
        log.debug("${CACHE_NAME}缓存同步完成，共影响${userIds.size}个用户。")
    }

    /**
     * 角色-资源关系变更后同步缓存（需要根据角色找到所有关联用户）。
     * 注意：受影响的用户包括 a) 直接拥有该角色的用户 b) 通过组继承该角色的用户。
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("角色${roleId}的资源关系变更后，同步${CACHE_NAME}缓存...")

        // 直接拥有该角色的用户
        val roleUserCriteria = Criteria(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
        val directUserIds = authRoleUserDao.search(roleUserCriteria).map { it.userId }.distinct()

        // 通过组继承该角色的用户：先找拥有该角色的所有 group，再展开为 userIds
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroupUserIds = groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }.distinct()

        val allUserIds = (directUserIds + viaGroupUserIds).distinct()
        if (allUserIds.isEmpty()) return
        syncByUserIds(allUserIds)
        log.debug(
            "${CACHE_NAME}缓存同步完成，共影响${allUserIds.size}个用户（" +
                "直接${directUserIds.size}+组继承${viaGroupUserIds.size}）。"
        )
    }

    /** 用户删除后清掉该 userId 下的 resourceId 列表。 */
    private fun evictByUserId(userId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, userId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleResourceRelationsChanged): Unit = syncOnRoleResourceChange(event.roleId)

    /** 用户进出组，整组对该用户的有效角色集合变化 → 重算资源。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncByUserIds(event.userIds)

    /** 组绑定的角色变了 → 整组内用户的资源都要重算。 */
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
