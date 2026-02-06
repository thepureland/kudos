package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.auth.core.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.dao.AuthRoleUserDao
import io.kudos.ms.user.core.cache.UserAccountHashCache
import io.kudos.ms.user.core.dao.UserAccountDao
import jakarta.annotation.Resource
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户资源ID列表（by tenant id & username）缓存处理器
 *
 * 1.数据来源表：user_account + auth_role_user + auth_role_resource
 * 2.缓存各租户下指定用户拥有的所有资源ID列表
 * 3.缓存的key为：tenantId::username
 * 4.缓存的value为：资源ID列表（List<String>）
 * 5.查询流程：用户 → 角色 → 资源（三级关联）
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
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的资源ID！")
            return
        }

        val users = userAccountDao.getActiveUsersForCache()
        val userIdToRoleIdsMap = authRoleUserDao.getAllUserIdToRoleIdsForCache()
        val roleIdToResourceIdsMap = authRoleResourceDao.getAllRoleIdToResourceIdsForCache()

        log.debug("从数据库加载了${users.size}条用户、角色-用户分组${userIdToRoleIdsMap.size}、角色-资源分组${roleIdToResourceIdsMap.size}。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户资源ID列表
        users.forEach { user ->
            val roleIds = userIdToRoleIdsMap[user.id!!] ?: emptyList()
            val resourceIds = roleIds.flatMap { roleId ->
                roleIdToResourceIdsMap[roleId] ?: emptyList()
            }.distinct()
            
            if (resourceIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, getKey(user.tenantId!!, user.username!!), resourceIds)
                log.debug("缓存了租户${user.tenantId}用户${user.username}的${resourceIds.size}条资源ID。")
            }
        }
    }

    /**
     * 根据租户ID和用户名从缓存中获取该用户拥有的所有资源ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return List<资源ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#username)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, username: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}用户${username}的资源ID，从数据库中加载...")
        }

        // 1. 从缓存中获取用户ID（避免查询数据库）
        val userId = userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id

        if (userId == null) {
            log.debug("找不到租户${tenantId}的用户${username}。")
            return emptyList()
        }

        val roleIds = authRoleUserDao.getRoleIdsByUserId(userId)
        if (roleIds.isEmpty()) {
            log.debug("用户${username}没有分配任何角色。")
            return emptyList()
        }

        val resultList = authRoleResourceDao.getResourceIdsByRoleIds(roleIds)
        log.debug("从数据库加载了租户${tenantId}用户${username}的${roleIds.size}个角色，共${resultList.size}条资源ID（去重后）。")
        return resultList
    }

    /**
     * 用户信息更新后同步缓存（用户名或状态变更）
     *
     * @param oldTenantId 旧租户ID
     * @param oldUsername 旧用户名
     * @param newTenantId 新租户ID（如果未变更则与旧值相同）
     * @param newUsername 新用户名（如果未变更则与旧值相同）
     */
    open fun syncOnUserUpdate(oldTenantId: String, oldUsername: String, newTenantId: String, newUsername: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户信息更新后，同步${CACHE_NAME}缓存...")
            
            // 踢除旧的缓存
            CacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldUsername))
            
            // 如果用户名或租户改变，也要踢除新的缓存（如果存在）
            if (oldTenantId != newTenantId || oldUsername != newUsername) {
                CacheKit.evict(CACHE_NAME, getKey(newTenantId, newUsername))
            }
            
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户-角色关系变更后同步缓存
     *
     * @param tenantId 租户ID
     * @param username 用户名
     */
    open fun syncOnRoleUserChange(tenantId: String, username: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户${username}的角色关系变更后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, username))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndUsernameCache>().getResourceIds(tenantId, username)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 角色-资源关系变更后同步缓存（需要根据角色找到所有关联用户）
     *
     * @param roleId 角色ID
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("角色${roleId}的资源关系变更后，同步${CACHE_NAME}缓存...")
            
            val userIds = authRoleUserDao.getUserIdsByRoleId(roleId)
            if (userIds.isNotEmpty()) {
                val users = userAccountDao.getUsersByIdsForCache(userIds)
                users.forEach { user ->
                    val t = user.tenantId ?: return@forEach
                    val u = user.username ?: return@forEach
                    CacheKit.evict(CACHE_NAME, getKey(t, u))
                    log.debug("踢除了用户$u 的资源缓存。")
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成，共影响${userIds.size}个用户。")
        }
    }

    /**
     * 用户删除后同步缓存
     *
     * @param tenantId 租户ID
     * @param username 用户名
     */
    open fun syncOnUserDelete(tenantId: String, username: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}用户${username}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(tenantId, username))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
        // 同时从用户 Hash 缓存中移除该用户，确保后续 getResourceIds 时按 tenantId+username 查不到已删除用户，返回空列表
        val userId = userAccountHashCache.getUsersByTenantIdAndUsername(tenantId, username)?.id
        userId?.let { userAccountHashCache.syncOnDelete(it) }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @return 缓存key
     */
    fun getKey(tenantId: String, username: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${username}"
    }

    private val log = LogFactory.getLog(this)

}
