package io.kudos.ms.user.core.contact.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.common.contact.vo.UserContactWayCacheEntry
import io.kudos.ms.user.common.contact.vo.request.UserContactWayQuery
import io.kudos.ms.user.core.contact.dao.UserContactWayDao
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * User contact way cache handler.
 *
 * 1. Source table: user_contact_way
 * 2. Caches only contact ways with active=true
 * 3. Cache key: user_id
 * 4. Cache value: list of UserContactWayCacheEntry
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
open class UserContactWayByUserIdCache : AbstractKeyValueCacheHandler<List<UserContactWayCacheEntry>>() {


    @Autowired
    private lateinit var userContactWayDao: UserContactWayDao

    companion object {
        private const val CACHE_NAME = "USER_CONTACT_WAY_BY_USER_ID"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<UserContactWayCacheEntry>? {
        return getSelf<UserContactWayByUserIdCache>().getContactWays(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("Cache disabled; not loading or caching active contact ways!")
            return
        }

        // Load all active contact ways.
        val searchPayload = UserContactWayQuery(active = true)
        @Suppress("UNCHECKED_CAST")
        val results = userContactWayDao.search(searchPayload, UserContactWayCacheEntry::class)
        log.debug("Loaded ${results.size} contact way records from the database.")

        // Clear the cache.
        if (clear) {
            clear()
        }

        // Cache the contact ways.
        val grouped = results.groupBy { it.userId }
        grouped.forEach { (userId, items) ->
            if (userId.isNullOrBlank()) return@forEach
            KeyValueCacheKit.put(CACHE_NAME, getKey(userId), items)
        }
        log.debug("Cached ${results.size} contact way records.")
    }

    /**
     * Get contact ways from the cache by user id; on miss, load from DB and write back.
     *
     * @param userId user id
     * @return List<UserContactWayCacheEntry>; an empty list when none are found
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getContactWays(userId: String): List<UserContactWayCacheEntry> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("Contact ways for user ${userId} not in cache; loading from DB...")
        }
        val searchPayload = UserContactWayQuery(
            userId = userId,
            active = true
        )
        @Suppress("UNCHECKED_CAST")
        val results = userContactWayDao.search(searchPayload, UserContactWayCacheEntry::class)
        if (results.isEmpty()) {
            log.warn("No active=true contact ways found in DB for user ${userId}!")
        } else {
            log.debug("Loaded ${results.size} contact way records from DB for user ${userId}.")
        }
        return results
    }

    /**
     * Sync the cache after a DB insert.
     *
     * @param any object containing the required properties
     * @param id contact way id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After inserting contact way id=${id}, syncing ${CACHE_NAME} cache...")
            val userId = BeanKit.getProperty(any, UserContactWay::userId.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, getKey(userId))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserContactWayByUserIdCache>().getContactWays(userId)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a DB update.
     *
     * @param any object containing the required properties
     * @param id contact way id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating contact way id=${id}, syncing ${CACHE_NAME} cache...")
            val userId = BeanKit.getProperty(any, UserContactWay::userId.name) as String
            KeyValueCacheKit.evict(CACHE_NAME, getKey(userId))
            if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserContactWayByUserIdCache>().getContactWays(userId)
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after the active flag is updated.
     *
     * @param id contact way id
     * @param active whether the contact way is active
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After updating active flag of contact way id=${id}, syncing cache...")
            val contactWay = userContactWayDao.get(id)
            if (contactWay == null) {
                log.warn("No contact way found with id=${id} while syncing cache.")
                return
            }
            val userId = contactWay.userId
            val key = getKey(userId)
            KeyValueCacheKit.evict(CACHE_NAME, key)
            if (active && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserContactWayByUserIdCache>().getContactWays(userId)
            }
            log.debug("Cache sync complete.")
        }
    }

    /**
     * Sync the cache after a DB delete.
     *
     * @param any object containing the required properties
     * @param id contact way id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            val userId = BeanKit.getProperty(any, UserContactWay::userId.name) as String
            log.debug("After deleting contact way id=${id}, evicting from ${CACHE_NAME} cache...")
            KeyValueCacheKit.evict(CACHE_NAME, getKey(userId))
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Sync the cache after a batch DB delete.
     *
     * @param ids contact way id collection
     * @param userIds user id collection
     */
    open fun syncOnBatchDelete(ids: Collection<String>, userIds: Collection<String>) {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("After batch deleting contact ways ids=${ids}, evicting from ${CACHE_NAME} cache...")
            userIds.distinct().forEach {
                KeyValueCacheKit.evict(CACHE_NAME, getKey(it))
            }
            log.debug("${CACHE_NAME} cache sync complete.")
        }
    }

    /**
     * Build the cache key by joining the given parameters.
     *
     * @param userId user id
     * @return cache key
     */
    fun getKey(userId: String): String {
        return userId
    }

    private val log = LogFactory.getLog(this::class)


}
