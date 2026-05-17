package io.kudos.ms.auth.core.group.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupBatchDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupDeleted
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 用户ID列表（by group id）缓存处理器
 *
 * 1.数据来源表：auth_group_user
 * 2.缓存各用户组拥有的所有用户ID列表
 * 3.缓存的key为：groupId
 * 4.缓存的value为：用户ID集合（List<String>）
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
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有用户组的用户ID！")
            return
        }

        val groups = authGroupDao.searchActiveGroupsForCache()
        val groupIdToUserIdsMap = authGroupUserDao.searchAllGroupIdToUserIdsForCache()

        log.debug("从数据库加载了${groups.size}条用户组、用户组-用户关系分组${groupIdToUserIdsMap.size}。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存用户组用户ID列表
        groups.forEach { group ->
            val groupId = group.id
            if (groupId.isBlank()) return@forEach
            val userIds = groupIdToUserIdsMap[groupId] ?: emptyList()
            if (userIds.isNotEmpty()) {
                KeyValueCacheKit.put(CACHE_NAME, groupId, userIds)
                log.debug("缓存了用户组${groupId}的${userIds.size}条用户ID。")
            }
        }
    }

    /**
     * 根据用户组ID从缓存中获取拥有该用户组的所有用户ID，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param groupId 用户组ID
     * @return Set<用户ID>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#groupId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getUserIds(groupId: String): List<String> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户组${groupId}的用户ID，从数据库中加载...")
        }

        val userIds = authGroupUserDao.searchUserIdsByGroupId(groupId)
        log.debug("从数据库加载了用户组${groupId}的${userIds.size}条用户ID。")
        return userIds.toList()
    }

    /**
     * 用户组-用户关系变更后同步缓存
     *
     * @param groupId 用户组ID
     */
    open fun syncOnGroupUserChange(groupId: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("用户组${groupId}的用户关系变更后，同步${CACHE_NAME}缓存...")
            evict(groupId)
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
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
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量用户组用户关系变更后，同步${CACHE_NAME}缓存...")
            groupIds.forEach { groupId ->
                KeyValueCacheKit.evict(CACHE_NAME, groupId)
                if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<UserIdsByGroupIdCache>().getUserIds(groupId)
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成，共影响${groupIds.size}个用户组。")
        }
    }

    /** 用户组删除后清掉该 groupId 下的 userId 列表。 */
    private fun evictByGroupId(groupId: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, groupId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupUserRelationsChanged): Unit = syncOnGroupUserChange(event.groupId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupDeleted): Unit = evictByGroupId(event.id)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: AuthGroupBatchDeleted) {
        event.ids.forEach(::evictByGroupId)
    }

    private val log = LogFactory.getLog(this::class)

}
