package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ms.auth.core.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.ms.auth.core.model.po.AuthGroupRole
import io.kudos.ms.auth.core.model.po.AuthRoleResource
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 资源ID列表（by tenant & group code）缓存处理器
 *
 * 1.数据来源表：auth_group + auth_group_role + auth_role_resource
 * 2.缓存各租户下指定用户组的资源ID列表
 * 3.缓存的key为：tenantId::groupCode
 * 4.缓存的value为：资源ID列表（List<String>）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class ResourceIdsByTenantIdAndGroupCodeCache : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var groupIdByTenantIdAndGroupCodeCache: GroupIdByTenantIdAndGroupCodeCache

    @Autowired
    private lateinit var authGroupDao: io.kudos.ms.auth.core.dao.AuthGroupDao

    @Autowired
    private lateinit var authGroupRoleDao: AuthGroupRoleDao

    @Autowired
    private lateinit var authRoleResourceDao: AuthRoleResourceDao

    companion object {
        private const val CACHE_NAME = "AUTH_RESOURCE_IDS_BY_TENANT_ID_AND_GROUP_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}用户组编码"
        }
        val tenantAndGroupCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<ResourceIdsByTenantIdAndGroupCodeCache>().getResourceIds(
            tenantAndGroupCode[0], tenantAndGroupCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户组下的资源ID！")
            return
        }

        // 加载所有active=true的用户组
        val groupCriteria = Criteria(AuthGroup::active.name, OperatorEnum.EQ, true)
        @Suppress("UNCHECKED_CAST")
        val groups = authGroupDao.search(groupCriteria)

        // 加载所有用户组-角色关系
        @Suppress("UNCHECKED_CAST")
        val allGroupRoles = authGroupRoleDao.allSearch()
        val groupIdToRoleIdsMap = allGroupRoles
            .groupBy { it.groupId }
            .mapValues { entry -> entry.value.map { it.roleId } }

        // 加载所有角色-资源关系
        @Suppress("UNCHECKED_CAST")
        val allRoleResources = authRoleResourceDao.allSearch()
        val roleIdToResourceIdsMap = allRoleResources
            .groupBy { it.roleId }
            .mapValues { entry -> entry.value.map { it.resourceId } }

        log.debug("从数据库加载了${groups.size}条用户组、${allGroupRoles.size}条组-角色关系、${allRoleResources.size}条角色-资源关系。")

        if (clear) {
            clear()
        }

        groups.forEach { group ->
            val roleIds = groupIdToRoleIdsMap[group.id!!] ?: emptyList()
            val resourceIds = roleIds.flatMap { roleId -> roleIdToResourceIdsMap[roleId] ?: emptyList() }.distinct()
            CacheKit.put(CACHE_NAME, getKey(group.tenantId, group.code), resourceIds)
            log.debug("缓存了租户${group.tenantId}用户组${group.code}的${resourceIds.size}条资源ID。")
        }
    }

    /**
     * 根据租户ID和用户组编码从缓存中获取该用户组下所有资源ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param tenantId 租户ID
     * @param groupCode 用户组编码
     * @return List<资源ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#groupCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getResourceIds(tenantId: String, groupCode: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}用户组${groupCode}的资源ID，从数据库中加载...")
        }

        // 1. 从缓存中获取用户组ID
        val groupId = groupIdByTenantIdAndGroupCodeCache.getGroupId(tenantId, groupCode)
        if (groupId == null) {
            log.debug("找不到租户${tenantId}的用户组${groupCode}。")
            return emptyList()
        }

        // 2. 获取用户组对应的角色ID列表
        val groupRoleCriteria = Criteria(AuthGroupRole::groupId.name, OperatorEnum.EQ, groupId)
        val roleIds = authGroupRoleDao.searchProperty(groupRoleCriteria, AuthGroupRole::roleId.name)
        @Suppress("UNCHECKED_CAST")
        val roleIdList = roleIds as List<String>
        if (roleIdList.isEmpty()) {
            return emptyList()
        }

        // 3. 根据角色ID查询资源ID列表
        val resourceIds = mutableListOf<String>()
        roleIdList.forEach { roleId ->
            val resourceCriteria = Criteria(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
            val ids = authRoleResourceDao.searchProperty(resourceCriteria, AuthRoleResource::resourceId.name)
            @Suppress("UNCHECKED_CAST")
            resourceIds.addAll(ids as List<String>)
        }

        log.debug("从数据库加载了租户${tenantId}用户组${groupCode}的${resourceIds.size}条资源ID。")
        return resourceIds.distinct()
    }

    /**
     * 用户组-角色关系新增后同步缓存
     */
    open fun syncOnGroupRoleInsert(tenantId: String, groupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增租户${tenantId}用户组${groupCode}的组-角色关系后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, groupCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndGroupCodeCache>().getResourceIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户组-角色关系删除后同步缓存
     */
    open fun syncOnGroupRoleDelete(tenantId: String, groupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}用户组${groupCode}的组-角色关系后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, groupCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<ResourceIdsByTenantIdAndGroupCodeCache>().getResourceIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 角色-资源关系变更后同步缓存
     */
    open fun syncOnRoleResourceChange(roleId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("角色${roleId}的资源关系变更后，同步${CACHE_NAME}缓存...")
            // 简化处理：清除所有缓存，避免复杂的反查
            clear()
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户组更新后同步缓存
     */
    open fun syncOnGroupUpdate(oldTenantId: String, oldGroupCode: String, newTenantId: String, newGroupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户组信息更新后，同步${CACHE_NAME}缓存...")
            CacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldGroupCode))
            if (oldTenantId != newTenantId || oldGroupCode != newGroupCode) {
                CacheKit.evict(CACHE_NAME, getKey(newTenantId, newGroupCode))
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户组删除后同步缓存
     */
    open fun syncOnGroupDelete(tenantId: String, groupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}用户组${groupCode}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(tenantId, groupCode))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
        groupIdByTenantIdAndGroupCodeCache.evict(groupIdByTenantIdAndGroupCodeCache.getKey(tenantId, groupCode))
    }

    fun getKey(tenantId: String, groupCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${groupCode}"
    }

    private val log = LogFactory.getLog(this)

}
