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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户ID列表（by group id）缓存处理器
 *
 * 1.数据来源表：auth_group_user
 * 2.缓存各用户组拥有的所有用户ID列表
 * 3.缓存的key为：groupId
 * 4.缓存的value为：用户ID列表（List<String>）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserIdsByGroupIdCache : AbstractKeyValueCacheHandler<List<String>>() {

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var authGroupDao: AuthGroupDao

    companion object {
        private const val CACHE_NAME = "AUTH_USER_IDS_BY_GROUP_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<UserIdsByGroupIdCache>().getUserIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户组的用户ID！")
            return
        }

        // 加载所有active=true的用户组
        val groupCriteria = Criteria(AuthGroup::active.name, OperatorEnum.EQ, true)
        val groups = authGroupDao.search(groupCriteria)

        // 加载所有用户组-用户关系
        val allGroupUsers = authGroupUserDao.allSearch()
        val groupIdToUserIdsMap = allGroupUsers
            .groupBy { it.groupId }
            .mapValues { entry -> entry.value.map { it.userId } }

        log.debug("从数据库加载了${groups.size}条用户组、${allGroupUsers.size}条用户组-用户关系。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户组用户ID列表
        groups.forEach { group ->
            val userIds = groupIdToUserIdsMap[group.id!!] ?: emptyList()
            if (userIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, group.id!!, userIds)
                log.debug("缓存了用户组${group.id}的${userIds.size}条用户ID。")
            }
        }
    }

    /**
     * 根据用户组ID从缓存中获取拥有该用户组的所有用户ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param groupId 用户组ID
     * @return List<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#groupId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(groupId: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户组${groupId}的用户ID，从数据库中加载...")
        }

        val groupUserCriteria = Criteria(AuthGroupUser::groupId.name, OperatorEnum.EQ, groupId)
        val userIds = authGroupUserDao.searchProperty(groupUserCriteria, AuthGroupUser::userId.name)

        log.debug("从数据库加载了用户组${groupId}的${userIds.size}条用户ID。")
        @Suppress("UNCHECKED_CAST")
        return userIds as List<String>
    }

    /**
     * 用户组-用户关系变更后同步缓存
     *
     * @param groupId 用户组ID
     */
    open fun syncOnGroupUserChange(groupId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户组${groupId}的用户关系变更后，同步${CACHE_NAME}缓存...")
            evict(groupId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserIdsByGroupIdCache>().getUserIds(groupId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量用户组-用户关系变更后同步缓存
     *
     * @param groupIds 用户组ID集合
     */
    open fun syncOnBatchGroupUserChange(groupIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户组用户关系变更后，同步${CACHE_NAME}缓存...")
            groupIds.forEach { groupId ->
                CacheKit.evict(CACHE_NAME, groupId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<UserIdsByGroupIdCache>().getUserIds(groupId)
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成，共影响${groupIds.size}个用户组。")
        }
    }

    /**
     * 用户组删除后同步缓存
     *
     * @param groupId 用户组ID
     */
    open fun syncOnGroupDelete(groupId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除用户组${groupId}后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, groupId)
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
