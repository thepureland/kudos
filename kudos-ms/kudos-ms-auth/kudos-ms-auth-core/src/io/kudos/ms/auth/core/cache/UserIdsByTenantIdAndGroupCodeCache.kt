package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ms.auth.core.dao.AuthGroupDao
import io.kudos.ms.auth.core.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.ms.auth.core.model.po.AuthGroupUser
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户ID列表（by tenant & group code）缓存处理器
 *
 * 1.数据来源表：auth_group + auth_group_user
 * 2.缓存各租户下指定用户组的用户ID列表
 * 3.缓存的key为：tenantId::groupCode
 * 4.缓存的value为：用户ID列表（List<String>）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserIdsByTenantIdAndGroupCodeCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authGroupHashCache: AuthGroupHashCache

    @Autowired
    private lateinit var authGroupDao: AuthGroupDao

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_TENANT_ID_AND_GROUP_CODE"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 租户ID${Consts.CACHE_KEY_DEFAULT_DELIMITER}用户组编码"
        }
        val tenantAndGroupCode = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<UserIdsByTenantIdAndGroupCodeCache>().getUserIds(
            tenantAndGroupCode[0], tenantAndGroupCode[1]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户组下的用户ID！")
            return
        }

        // 加载所有active=true的用户组
        val groupCriteria = Criteria(AuthGroup::active.name, OperatorEnum.EQ, true)

        @Suppress("UNCHECKED_CAST")
        val groups = authGroupDao.search(groupCriteria)

        // 加载所有用户组-用户关系
        @Suppress("UNCHECKED_CAST")
        val allGroupUsers = authGroupUserDao.allSearch()
        val groupIdToUserIdsMap = allGroupUsers
            .groupBy { it.groupId }
            .mapValues { entry -> entry.value.map { it.userId } }

        log.debug("从数据库加载了${groups.size}条用户组和${allGroupUsers.size}条用户组-用户关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户ID列表
        groups.forEach { group ->
            val userIds = groupIdToUserIdsMap[group.id!!] ?: emptyList()
            CacheKit.put(CACHE_NAME, getKey(group.tenantId, group.code), userIds)
            log.debug("缓存了租户${group.tenantId}用户组${group.code}的${userIds.size}条用户ID。")
        }
    }

    /**
     * 根据租户ID和用户组编码从缓存中获取该用户组下所有用户ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param tenantId 租户ID
     * @param groupCode 用户组编码
     * @return List<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#groupCode)",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(tenantId: String, groupCode: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在租户${tenantId}用户组${groupCode}的用户ID，从数据库中加载...")
        }

        // 1. 从缓存中获取用户组ID（避免查询数据库）
        val groupId = authGroupHashCache.getGroupByTenantIdAndGroupCode(tenantId, groupCode)?.id

        if (groupId == null) {
            log.debug("找不到租户${tenantId}的用户组${groupCode}。")
            return emptyList()
        }

        // 2. 根据用户组ID查询用户ID列表
        val userCriteria = Criteria(AuthGroupUser::groupId.name, OperatorEnum.EQ, groupId)
        val userIds = authGroupUserDao.searchProperty(userCriteria, AuthGroupUser::userId.name)

        log.debug("从数据库加载了租户${tenantId}用户组${groupCode}的${userIds.size}条用户ID。")
        @Suppress("UNCHECKED_CAST")
        return userIds as List<String>
    }

    /**
     * 用户组-用户关系插入后同步缓存
     *
     * @param tenantId 租户ID
     * @param groupCode 用户组编码
     */
    open fun syncOnGroupUserInsert(tenantId: String, groupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增租户${tenantId}用户组${groupCode}的用户关系后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, groupCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByTenantIdAndGroupCodeCache>().getUserIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户组-用户关系删除后同步缓存
     *
     * @param tenantId 租户ID
     * @param groupCode 用户组编码
     */
    open fun syncOnGroupUserDelete(tenantId: String, groupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}用户组${groupCode}的用户关系后，同步${CACHE_NAME}缓存...")
            evict(getKey(tenantId, groupCode))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByTenantIdAndGroupCodeCache>().getUserIds(tenantId, groupCode)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户组信息更新后同步缓存（用户组编码或状态变更）
     *
     * @param oldTenantId 旧租户ID
     * @param oldGroupCode 旧用户组编码
     * @param newTenantId 新租户ID（如果未变更则与旧值相同）
     * @param newGroupCode 新用户组编码（如果未变更则与旧值相同）
     */
    open fun syncOnGroupUpdate(oldTenantId: String, oldGroupCode: String, newTenantId: String, newGroupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户组信息更新后，同步${CACHE_NAME}缓存...")

            // 踢除旧的缓存
            CacheKit.evict(CACHE_NAME, getKey(oldTenantId, oldGroupCode))

            // 如果编码或租户改变，也要踢除新的缓存（如果存在）
            if (oldTenantId != newTenantId || oldGroupCode != newGroupCode) {
                CacheKit.evict(CACHE_NAME, getKey(newTenantId, newGroupCode))
            }

            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 用户组删除后同步缓存
     *
     * @param tenantId 租户ID
     * @param groupCode 用户组编码
     */
    open fun syncOnGroupDelete(tenantId: String, groupCode: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户${tenantId}用户组${groupCode}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(tenantId, groupCode))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
        // 同时清除用户组ID缓存，确保后续查询时不会从缓存中获取到已删除的用户组ID
        val groupId = authGroupHashCache.getGroupByTenantIdAndGroupCode(tenantId, groupCode)?.id
        groupId?.let { authGroupHashCache.syncOnDelete(groupId) }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param tenantId 租户ID
     * @param groupCode 用户组编码
     * @return 缓存key
     */
    fun getKey(tenantId: String, groupCode: String): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${groupCode}"
    }

    private val log = LogFactory.getLog(this)

}
