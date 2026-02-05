package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.dao.AuthRoleDao
import io.kudos.ms.auth.core.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.model.po.AuthRole
import io.kudos.ms.auth.core.model.po.AuthRoleResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 资源ID列表（by role id）缓存处理器
 *
 * 1.数据来源表：auth_role_resource
 * 2.缓存各角色拥有的所有资源ID列表
 * 3.缓存的key为：roleId
 * 4.缓存的value为：资源ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByRoleIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_ROLE_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<ResourceIdsByRoleIdCache>().getResourceIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有角色的资源ID！")
            return
        }

        // 加载所有active=true的角色
        val roleCriteria = Criteria(AuthRole::active.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val roles = authRoleDao.search(roleCriteria)
        
        // 加载所有角色-资源关系
        @Suppress("UNCHECKED_CAST")
        val allRoleResources = authRoleResourceDao.allSearch()
        val roleIdToResourceIdsMap = allRoleResources
            .groupBy { it.roleId }
            .mapValues { entry -> entry.value.map { it.resourceId.trim() } }

        log.debug("从数据库加载了${roles.size}条角色、${allRoleResources.size}条角色-资源关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存角色资源ID列表
        roles.forEach { role ->
            val resourceIds = roleIdToResourceIdsMap[role.id!!] ?: emptyList()
            if (resourceIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, role.id!!, resourceIds)
                log.debug("缓存了角色${role.id}的${resourceIds.size}条资源ID。")
            }
        }
    }

    /**
     * 根据角色ID从缓存中获取该角色拥有的所有资源ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param roleId 角色ID
     * @return List<资源ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#roleId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(roleId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在角色${roleId}的资源ID，从数据库中加载...")
        }

        val roleResourceCriteria = Criteria(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
        val resourceIds = authRoleResourceDao.searchProperty(roleResourceCriteria, AuthRoleResource::resourceId.name)
        
        log.debug("从数据库加载了角色${roleId}的${resourceIds.size}条资源ID。")
        @Suppress("UNCHECKED_CAST")
        return (resourceIds as List<String>).map { it.trim() }
    }

    /**
     * 角色-资源关系变更后同步缓存
     *
     * @param roleId 角色ID
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("角色${roleId}的资源关系变更后，同步${CACHE_NAME}缓存...")
            evict(roleId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByRoleIdCache>().getResourceIds(roleId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量角色-资源关系变更后同步缓存
     *
     * @param roleIds 角色ID集合
     */
    open fun syncOnBatchRoleResourceChange(roleIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量角色资源关系变更后，同步${CACHE_NAME}缓存...")
            roleIds.forEach { roleId ->
                CacheKit.evict(CACHE_NAME, roleId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<ResourceIdsByRoleIdCache>().getResourceIds(roleId)
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成，共影响${roleIds.size}个角色。")
        }
    }

    /**
     * 角色删除后同步缓存
     *
     * @param roleId 角色ID
     */
    open fun syncOnRoleDelete(roleId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除角色${roleId}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, roleId)
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
