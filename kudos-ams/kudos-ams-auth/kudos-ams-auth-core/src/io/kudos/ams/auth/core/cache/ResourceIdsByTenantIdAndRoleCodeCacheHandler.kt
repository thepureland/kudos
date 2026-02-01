package io.kudos.ams.auth.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.auth.core.dao.AuthRoleDao
import io.kudos.ams.auth.core.dao.AuthRoleResourceDao
import io.kudos.ams.auth.core.model.po.AuthRole
import io.kudos.ams.auth.core.model.po.AuthRoleResource
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

/**
 * 资源ID列表（by tenant & role code）缓存处理器
 *
 * 1.数据来源表：auth_role + auth_role_resource
 * 2.缓存各租户下指定角色的资源ID列表
 * 3.缓存的key为：tenantId::roleCode
 * 4.缓存的value为：资源ID列表（List<String>）
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class ResourceIdsByTenantIdAndRoleCodeCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var roleIdByTenantIdAndRoleCodeCacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    @Autowired
    private lateinit var authRoleDao: AuthRoleDao

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_ROLE_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}角色编码"
        }
        val tenantAndRoleCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdsByTenantIdAndRoleCodeCacheHandler>().getResourceIds(
            tenantAndRoleCode[0], tenantAndRoleCode[1]
        )
    }

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

        log.debug("从数据库加载了${roles.size}条角色和${allRoleResources.size}条角色-资源关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存资源ID列表
        roles.forEach { role ->
            val resourceIds = roleIdToResourceIdsMap[role.id!!] ?: emptyList()
            CacheKit.put(CACHE_NAME, getKey(role.tenantId, role.code), resourceIds)
            log.debug("缓存了租户${role.tenantId}角色${role.code}的${resourceIds.size}条资源ID。")
        }
    }

    /**
     * 根据租户ID和角色编码从缓存中获取该角色拥有的所有资源ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return List<资源ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#roleCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, roleCode: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}角色${roleCode}的资源ID，从数据库中加载...")
        }

        // 1. 从缓存中获取角色ID（避免查询数据库）
        val roleId = roleIdByTenantIdAndRoleCodeCacheHandler.getRoleId(tenantId, roleCode)

        if (roleId == null) {
            log.debug("找不到租户${tenantId}的角色${roleCode}。")
            return emptyList()
        }

        // 2. 根据角色ID查询资源ID列表
        val resourceCriteria = Criteria(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
        val resourceIds = authRoleResourceDao.searchProperty(resourceCriteria, AuthRoleResource::resourceId.name)

        log.debug("从数据库加载了租户${tenantId}角色${roleCode}的${resourceIds.size}条资源ID。")
        @Suppress("UNCHECKED_CAST")
        return (resourceIds as List<String>).map { it.trim() }
    }

    /**
     * 角色-资源关系插入后同步缓存
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     */
    open fun syncOnRoleResourceInsert(tenantId: String, roleCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增租户${tenantId}角色${roleCode}的资源关系后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, roleCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndRoleCodeCacheHandler>().getResourceIds(tenantId, roleCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 角色-资源关系删除后同步缓存
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     */
    open fun syncOnRoleResourceDelete(tenantId: String, roleCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}角色${roleCode}的资源关系后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, roleCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndRoleCodeCacheHandler>().getResourceIds(tenantId, roleCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 角色信息更新后同步缓存（角色编码或状态变更）
     *
     * @param oldTenantId 旧租户ID
     * @param oldRoleCode 旧角色编码
     * @param newTenantId 新租户ID（如果未变更则与旧值相同）
     * @param newRoleCode 新角色编码（如果未变更则与旧值相同）
     */
    open fun syncOnRoleUpdate(oldTenantId: String, oldRoleCode: String, newTenantId: String, newRoleCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("角色信息更新后，同步${CACHE_NAME}缓存...")

            // 踢除旧的缓存
            CacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldRoleCode))

            // 如果编码或租户改变，也要踢除新的缓存（如果存在）
            if (oldTenantId != newTenantId || oldRoleCode != newRoleCode) {
                CacheKit.evict(CACHE_NAME, getKey(newTenantId, newRoleCode))
            }

            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 角色删除后同步缓存
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     */
    open fun syncOnRoleDelete(tenantId: String, roleCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}角色${roleCode}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(tenantId, roleCode))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
        // 同时清除角色ID缓存，确保后续查询时不会从缓存中获取到已删除的角色ID
        roleIdByTenantIdAndRoleCodeCacheHandler.evict(roleIdByTenantIdAndRoleCodeCacheHandler.getKey(tenantId, roleCode))
    }

    /**
     * 返回参数拼接后的key
     *
     * @param tenantId 租户ID
     * @param roleCode 角色编码
     * @return 缓存key
     */
    fun getKey(tenantId: String, roleCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${roleCode}"
    }

    private val log = LogFactory.getLog(this)

}