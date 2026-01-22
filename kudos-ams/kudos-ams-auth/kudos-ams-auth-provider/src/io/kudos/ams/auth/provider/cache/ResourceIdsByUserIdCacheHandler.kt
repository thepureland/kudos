package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.auth.provider.dao.AuthRoleResourceDao
import io.kudos.ams.auth.provider.dao.AuthRoleUserDao
import io.kudos.ams.auth.provider.dao.AuthUserDao
import io.kudos.ams.auth.provider.model.po.AuthRoleResource
import io.kudos.ams.auth.provider.model.po.AuthRoleUser
import io.kudos.ams.auth.provider.model.po.AuthUser
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 资源ID列表（by user id）缓存处理器
 *
 * 1.数据来源表：auth_role_user + auth_role_resource
 * 2.缓存各用户拥有的所有资源ID列表
 * 3.缓存的key为：userId
 * 4.缓存的value为：资源ID列表（List<String>）
 * 5.查询流程：用户 → 角色 → 资源（三级关联）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByUserIdCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Autowired
    private lateinit var authUserDao: AuthUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<ResourceIdsByUserIdCacheHandler>().getResourceIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的资源ID！")
            return
        }

        // 加载所有active=true的用户
        val userCriteria = Criteria(AuthUser::active.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val users = authUserDao.search(userCriteria) as List<AuthUser>
        
        // 加载所有角色-用户关系
        @Suppress("UNCHECKED_CAST")
        val allRoleUsers = authRoleUserDao.allSearch()
        val userIdToRoleIdsMap = allRoleUsers
            .groupBy { it.userId }
            .mapValues { entry -> entry.value.map { it.roleId } }
        
        // 加载所有角色-资源关系
        @Suppress("UNCHECKED_CAST")
        val allRoleResources = authRoleResourceDao.allSearch()
        val roleIdToResourceIdsMap = allRoleResources
            .groupBy { it.roleId }
            .mapValues { entry -> entry.value.map { it.resourceId } }

        log.debug("从数据库加载了${users.size}条用户、${allRoleUsers.size}条角色-用户关系、${allRoleResources.size}条角色-资源关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户资源ID列表
        users.forEach { user ->
            val roleIds = userIdToRoleIdsMap[user.id!!] ?: emptyList()
            val resourceIds = roleIds.flatMap { roleId ->
                roleIdToResourceIdsMap[roleId] ?: emptyList()
            }.map { it.trim() }
            .distinct()
            
            if (resourceIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, user.id!!, resourceIds)
                log.debug("缓存了用户${user.id}的${resourceIds.size}条资源ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户拥有的所有资源ID，如果缓存中不存在，则从数据库中加载，并回写缓存
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
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的资源ID，从数据库中加载...")
        }

        // 1. 根据用户ID查询角色ID列表
        val roleUserCriteria = Criteria(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        val roleIds = authRoleUserDao.searchProperty(roleUserCriteria, AuthRoleUser::roleId.name)
        
        if (roleIds.isEmpty()) {
            log.debug("用户${userId}没有分配任何角色。")
            return emptyList()
        }

        // 2. 根据角色ID列表查询资源ID列表（支持批量查询）
        @Suppress("UNCHECKED_CAST")
        val allRoleIds = roleIds as List<String>
        val resourceIdSet = mutableSetOf<String>()
        
        allRoleIds.forEach { roleId ->
            val roleResourceCriteria = Criteria(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
            val resourceIds = authRoleResourceDao.searchProperty(roleResourceCriteria, AuthRoleResource::resourceId.name)
            @Suppress("UNCHECKED_CAST")
            resourceIdSet.addAll((resourceIds as List<String>).map { it.trim() })
        }
        
        val resultList = resourceIdSet.toList()
        log.debug("从数据库加载了用户${userId}的${allRoleIds.size}个角色，共${resultList.size}条资源ID（去重后）。")
        return resultList
    }

    /**
     * 用户-角色关系变更后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnRoleUserChange(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户${userId}的角色关系变更后，同步${CACHE_NAME}缓存...")
            evict(userId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByUserIdCacheHandler>().getResourceIds(userId)
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
            
            // 查询拥有该角色的所有用户
            val roleUserCriteria = Criteria(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
            @Suppress("UNCHECKED_CAST")
            val roleUsers = authRoleUserDao.search(roleUserCriteria)
            
            // 踢除这些用户的缓存
            val userIds = roleUsers.map { it.userId }.distinct()
            userIds.forEach { userId ->
                CacheKit.evict(CACHE_NAME, userId)
                log.debug("踢除了用户${userId}的资源缓存。")
            }
            
            log.debug("${CACHE_NAME}缓存同步完成，共影响${userIds.size}个用户。")
        }
    }

    /**
     * 用户删除后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnUserDelete(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除用户${userId}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, userId)
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量用户-角色关系变更后同步缓存
     *
     * @param userIds 用户ID集合
     */
    open fun syncOnBatchRoleUserChange(userIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户角色关系变更后，同步${CACHE_NAME}缓存...")
            userIds.forEach { userId ->
                CacheKit.evict(CACHE_NAME, userId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<ResourceIdsByUserIdCacheHandler>().getResourceIds(userId)
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成，共影响${userIds.size}个用户。")
        }
    }

    private val log = LogFactory.getLog(this)

}
