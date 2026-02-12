package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.dao.AuthGroupUserDao
import io.kudos.ms.user.core.dao.UserAccountDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户组ID列表（by user id）缓存处理器
 *
 * 1.数据来源表：auth_group_user
 * 2.缓存各用户拥有的所有用户组ID列表
 * 3.缓存的key为：userId
 * 4.缓存的value为：用户组ID集合（Set<String>）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class GroupIdsByUserIdCache : AbstractKeyValueCacheHandler<Set<String>>() {

    @Autowired
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Autowired
    private lateinit var userAccountDao: UserAccountDao

    companion object {
        private const val CACHE_NAME = "AUTH_GROUP_IDS_BY_USER_ID"
    }

    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): Set<String> = getSelf<GroupIdsByUserIdCache>().getGroupIds(key)

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户的用户组ID！")
            return
        }

        val users = userAccountDao.searchActiveUsersForCache()
        val userIdToGroupIdsMap = authGroupUserDao.searchAllUserIdToGroupIdsForCache()

        log.debug("从数据库加载了${users.size}条用户、用户组-用户关系分组${userIdToGroupIdsMap.size}。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户组ID列表
        users.forEach { user ->
            val userId = user.id
            if (userId.isBlank()) return@forEach
            val groupIds = userIdToGroupIdsMap[userId] ?: emptyList()
            if (groupIds.isNotEmpty()) {
                CacheKit.put(CACHE_NAME, userId, groupIds)
                log.debug("缓存了用户${userId}的${groupIds.size}条用户组ID。")
            }
        }
    }

    /**
     * 根据用户ID从缓存中获取该用户拥有的所有用户组ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param userId 用户ID
     * @return Set<用户组ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getGroupIds(userId: String): Set<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的用户组ID，从数据库中加载...")
        }

        val groupIds = authGroupUserDao.searchGroupIdsByUserId(userId)
        log.debug("从数据库加载了用户${userId}的${groupIds.size}条用户组ID。")
        return groupIds
    }

    /**
     * 用户-用户组关系变更后同步缓存
     *
     * @param userId 用户ID
     */
    open fun syncOnGroupUserChange(userId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户${userId}的用户组关系变更后，同步${CACHE_NAME}缓存...")
            evict(userId)
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<GroupIdsByUserIdCache>().getGroupIds(userId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量用户-用户组关系变更后同步缓存
     *
     * @param userIds 用户ID集合
     */
    open fun syncOnBatchGroupUserChange(userIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户用户组关系变更后，同步${CACHE_NAME}缓存...")
            userIds.forEach { userId ->
                CacheKit.evict(CACHE_NAME, userId)
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<GroupIdsByUserIdCache>().getGroupIds(userId)
                }
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

    private val log = LogFactory.getLog(this)

}
