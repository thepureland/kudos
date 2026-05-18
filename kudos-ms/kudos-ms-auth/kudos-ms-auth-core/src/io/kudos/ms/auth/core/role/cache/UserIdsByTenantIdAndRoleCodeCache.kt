package io.kudos.ms.auth.core.role.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupRoleRelationsChanged
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleBatchDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleDeleted
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 用户ID列表（by tenant & role code）缓存处理器。
 *
 * **语义为"有效用户集合"**（与 [UserIdsByRoleIdCache] 平行）：
 * - 数据来源：`auth_role` ∪ `auth_role_user`（直接）∪ `auth_group_role` + `auth_group_user`（组继承）
 * - 缓存的 value 是直接绑定 + 组继承的用户 ID 列表（去重）
 *
 * 缓存 key：tenantId::roleCode；value：List<String>。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class UserIdsByTenantIdAndRoleCodeCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Resource
    private lateinit var authRoleHashCache: AuthRoleHashCache

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Resource
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_TENANT_ID_AND_ROLE_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}角色编码"
        }
        val tenantAndRoleCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<UserIdsByTenantIdAndRoleCodeCache>().getUserIds(
            tenantAndRoleCode[0], tenantAndRoleCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有角色下的用户ID！")
            return
        }

        val roles = authRoleDao.searchActiveRolesForCache()
        val roleIdToDirectUserIds = authRoleUserDao.getAllRoleIdToUserIdsForCache()
        val groupIdToRoleIds = authGroupRoleDao.searchAllGroupIdToRoleIdsForCache()
        val groupIdToUserIds = authGroupUserDao.searchAllGroupIdToUserIdsForCache()
        val roleIdToGroupIds: Map<String, List<String>> = buildMap<String, MutableList<String>> {
            groupIdToRoleIds.forEach { (groupId, roleIds) ->
                roleIds.forEach { roleId -> getOrPut(roleId) { mutableListOf() }.add(groupId) }
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
            val tenantId = role.tenantId ?: return@forEach
            val roleCode = role.code ?: return@forEach
            val direct = roleIdToDirectUserIds[roleId].orEmpty()
            val viaGroup = roleIdToGroupIds[roleId].orEmpty().flatMap { gid ->
                groupIdToUserIds[gid].orEmpty()
            }
            val all = (direct + viaGroup).distinct()
            KeyValueCacheKit.put(CACHE_NAME, getKey(tenantId, roleCode), all)
            log.debug(
                "缓存了租户${tenantId}角色${roleCode}的${all.size}条用户ID（直接${direct.size}+组继承${viaGroup.size}）。"
            )
        }
    }

    /**
     * 根据租户ID和角色编码从缓存中获取该角色下所有用户ID（直接绑定 ∪ 组继承）。
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return List<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#roleCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(tenantId: String, roleCode: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}角色${roleCode}的用户ID，从数据库中加载...")
        }

        val roleId = authRoleHashCache.getRoleByTenantIdAndRoleCode(tenantId, roleCode)?.id
        if (roleId == null) {
            log.debug("找不到租户${tenantId}的角色${roleCode}。")
            return emptyList()
        }

        val direct = authRoleUserDao.searchUserIdsByRoleId(roleId)
        val groupIds = authGroupRoleDao.searchGroupIdsByRoleId(roleId)
        val viaGroup = if (groupIds.isEmpty()) emptyList() else {
            groupIds.flatMap { gid -> authGroupUserDao.searchUserIdsByGroupId(gid) }
        }
        val effective = (direct + viaGroup).distinct()
        log.debug(
            "从数据库加载了租户${tenantId}角色${roleCode}的有效用户：直接${direct.size}，组继承${viaGroup.size}，去重${effective.size}。"
        )
        return effective
    }

    /**
     * 角色-用户关系插入后同步缓存（向后兼容入口；新代码请走事件 [AuthRoleUserRelationsChanged]）。
     */
    open fun syncOnRoleUserInsert(tenantId: String, roleCode: String) = syncOnRoleUserDelete(tenantId, roleCode)

    /**
     * 角色-用户关系删除后同步缓存（向后兼容入口；新代码请走事件）。
     */
    open fun syncOnRoleUserDelete(tenantId: String, roleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("租户${tenantId}角色${roleCode}的用户关系变更后，同步${CACHE_NAME}缓存...")
        evict(getKey(tenantId, roleCode))
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<UserIdsByTenantIdAndRoleCodeCache>().getUserIds(tenantId, roleCode)
        }
        log.debug("${CACHE_NAME}缓存同步完成。")
    }

    /**
     * 角色信息更新后同步缓存（角色编码或租户变更）。
     */
    open fun syncOnRoleUpdate(oldTenantId: String, oldRoleCode: String, newTenantId: String, newRoleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        log.debug("角色信息更新后，同步${CACHE_NAME}缓存...")

        KeyValueCacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldRoleCode))
        if (oldTenantId != newTenantId || oldRoleCode != newRoleCode) {
            KeyValueCacheKit.evict(CACHE_NAME, getKey(newTenantId, newRoleCode))
        }
        log.debug("${CACHE_NAME}缓存同步完成。")
    }

    fun getKey(tenantId: String, roleCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${roleCode}"
    }

    /** 仅做本地 (tenantId, roleCode) 失效；AuthRoleHashCache 已独立订阅 AuthRoleDeleted。 */
    private fun evictBy(tenantId: String, roleCode: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, roleCode))
    }

    /**
     * 按一组 roleId 反查 (tenantId, code) 并失效。每个 roleId 一次 hash-cache lookup ——
     * 通常 ≤ 几十个，可接受；若改为大批量再优化。
     */
    private fun evictByRoleIds(roleIds: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        roleIds.forEach { roleId ->
            val role = authRoleHashCache.getRoleById(roleId)
            val tenantId = role?.tenantId
            val code = role?.code
            if (tenantId != null && code != null) {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(tenantId, code))
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleDeleted): Unit = evictBy(event.tenantId, event.code)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleBatchDeleted) {
        event.items.forEach { evictBy(it.tenantId, it.code) }
    }

    /** 角色-用户直接关系变更 → 失效该 roleId 对应的 (tenantId, code)。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthRoleUserRelationsChanged): Unit = evictByRoleIds(listOf(event.roleId))

    /** 用户进出组 → 该组所有 roleId 对应的 (tenantId, code) 都要失效。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val roleIds = authGroupRoleDao.searchRoleIdsByGroupId(event.groupId)
        if (roleIds.isEmpty()) return
        evictByRoleIds(roleIds)
    }

    /** 组绑定的角色变了 → 这些角色的 (tenantId, code) 要失效。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupRoleRelationsChanged): Unit = evictByRoleIds(event.roleIds)

    private val log = LogFactory.getLog(this::class)

}
