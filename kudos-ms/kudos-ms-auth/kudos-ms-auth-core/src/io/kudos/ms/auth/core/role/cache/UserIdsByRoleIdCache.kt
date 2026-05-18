package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 用户ID列表（by role id）缓存处理器。
 *
 * **语义为"有效用户集合"**：
 * - 数据来源表：`auth_role_user`（直接绑定）∪ `auth_group_role` + `auth_group_user`（组继承）
 * - 缓存的 value 是这两条路径 union 后的用户 ID 列表（去重）
 *
 * 之所以也算组继承的用户，是因为问"该角色有哪些用户"的语义在权限判定上等价于
 * "该角色实际作用于哪些用户" —— 不应该把通过组继承的用户排除在外。
 * 需要"仅直接绑定该角色的用户"的调用方应直接走 [AuthRoleUserDao.searchUserIdsByRoleId]。
 *
 * 缓存 key：roleId；value：List<String>。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByRoleIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_ROLE_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByRoleIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有角色的用户ID！")
            return
        }

        val roles = authRoleDao.searchActiveRolesForCache()
        val roleIdToDirectUserIds = authRoleUserDao.getAllRoleIdToUserIdsForCache()
        // 倒置 group→roles 为 role→groups，把"该角色被哪些组持有"做成 map 查询
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val groupIdToUserIds = authGroupUserDao.searchAllGroupIdToUserIdsForCache()
        val roleIdToGroupIds: Map<String, List<String>> = buildMap<String, MutableList<String>> {
            groupIdToRoleIds.forEach { (groupId, roleIds) ->
                roleIds.forEach { roleId ->
                    getOrPut(roleId) { mutableListOf() }.add(groupId)
                }
            }
        }

        log.debug(
            "从数据库加载了${roles.size}条角色、直接角色-用户分组${roleIdToDirectUserIds.size}、" +
                "组-角色分组${groupIdToRoleIds.size}、组-用户分组${groupIdToUserIds.size}。"
        )

        if (clear) {
            clear()
        }

        roles.forEach { role ->
            val roleId = role.id
            if (roleId.isBlank()) return@forEach
            val direct = roleIdToDirectUserIds[roleId].orEmpty()
            val viaGroup = roleIdToGroupIds[roleId].orEmpty().flatMap { gid ->
                groupIdToUserIds[gid].orEmpty()
            }
            val all = (direct + viaGroup).distinct()
            if (all.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, roleId, all)
                log.debug("缓存了角色${roleId}的${all.size}条有效用户ID（直接${direct.size}+组继承${viaGroup.size}）。")
            }
        }
    }

    /**
     * 根据角色ID从缓存中获取拥有该角色的所有用户ID（直接绑定 ∪ 组继承），缓存不存在时从数据库加载并回写。
     *
     * @param roleId 角色ID
     * @return List<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#roleId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(roleId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在角色${roleId}的用户ID，从数据库中加载...")
        }
        val direct = authRoleUserDao.searchUserIdsByRoleId(roleId)
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroup = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }
        }
        val effective = (direct + viaGroup).distinct()
        log.debug(
            "从数据库加载了角色${roleId}的有效用户：直接${direct.size}，组继承${viaGroup.size}，去重${effective.size}。"
        )
        return effective
    }

    /**
     * @deprecated 保留单角色公开入口做向后兼容；新代码请走事件机制。
     */
    open fun syncOnRoleUserChange(roleId: String): Unit = syncByRoleIds(listOf(roleId))

    /**
     * @deprecated 保留批量公开入口做向后兼容；新代码请走事件机制。
     */
    open fun syncOnBatchRoleUserChange(roleIds: Collection<String>): Unit = syncByRoleIds(roleIds)

    private fun syncByRoleIds(roleIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        roleIds.forEach { roleId ->
            KeyValueCacheKit.evict(CACHE_NAME, roleId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByRoleIdCache>().getUserIds(roleId)
            }
        }
        log.debug("${CACHE_NAME}缓存同步完成，共影响${roleIds.size}个角色。")
    }

    /** 角色删除后清掉该 roleId 下的 userId 列表。 */
    private fun evictByRoleId(roleId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, roleId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = syncByRoleIds(listOf(event.roleId))

    /**
     * 用户进出某个组 → 该组关联的所有角色的"有效用户集合"都要重算。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val roleIds = authGroupRoleDao.searchRoleIdsByGroupId(event.groupId)
        if (roleIds.isEmpty()) return
        syncByRoleIds(roleIds)
    }

    /**
     * 组关联的角色变了 → 这些角色的"有效用户集合"都要重算。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged): Unit = syncByRoleIds(event.roleIds)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = evictByRoleId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted) {
        event.ids.forEach(::evictByRoleId)
    }

    private val log = LogFactory.getLog(this::class)

}
