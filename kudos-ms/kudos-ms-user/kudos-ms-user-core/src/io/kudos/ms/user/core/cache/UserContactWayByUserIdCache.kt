package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ms.user.common.vo.contact.UserContactWayCacheItem
import io.kudos.ms.user.common.vo.contact.UserContactWaySearchPayload
import io.kudos.ms.user.core.dao.UserContactWayDao
import io.kudos.ms.user.core.model.po.UserContactWay
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 用户联系方式缓存处理器
 *
 * 1.数据来源表：user_contact_way
 * 2.仅缓存active=true的联系方式
 * 3.缓存的key为：user_id
 * 4.缓存的value为：UserContactWayCacheItem对象列表
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Component
//region your codes 1
open class UserContactWayByUserIdCache : AbstractCacheHandler<List<UserContactWayCacheItem>>() {
//endregion your codes 1

    //region your codes 2

    @Autowired
    private lateinit var userContactWayDao: UserContactWayDao

    companion object {
        private const val CACHE_NAME = "USER_CONTACT_WAY_BY_USER_ID"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<UserContactWayCacheItem>? {
        return getSelf<UserContactWayByUserIdCache>().getContactWays(key)
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的联系方式！")
            return
        }

        // 加载所有可用的联系方式
        val searchPayload = UserContactWaySearchPayload().apply {
            returnEntityClass = UserContactWayCacheItem::class
            active = true
        }
        @Suppress("UNCHECKED_CAST")
        val results = userContactWayDao.search(searchPayload) as List<UserContactWayCacheItem>
        log.debug("从数据库加载了${results.size}条联系方式。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存联系方式
        val grouped = results.groupBy { it.userId }
        grouped.forEach { (userId, items) ->
            if (userId.isNullOrBlank()) return@forEach
            CacheKit.put(CACHE_NAME, getKey(userId), items)
        }
        log.debug("缓存了${results.size}条联系方式。")
    }

    /**
     * 根据用户ID从缓存获取联系方式，如果缓存中不存在，则从数据库加载并写入缓存
     *
     * @param userId 用户ID
     * @return List<UserContactWayCacheItem>，找不到返回空列表
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#userId",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getContactWays(userId: String): List<UserContactWayCacheItem> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在用户${userId}的联系方式，从数据库中加载...")
        }
        val searchPayload = UserContactWaySearchPayload().apply {
            returnEntityClass = UserContactWayCacheItem::class
            this.userId = userId
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        val results = userContactWayDao.search(searchPayload) as List<UserContactWayCacheItem>
        if (results.isEmpty()) {
            log.warn("数据库中不存在用户${userId}的active=true的联系方式！")
        } else {
            log.debug("数据库中加载到用户${userId}的联系方式共${results.size}条。")
        }
        return results
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 联系方式id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的联系方式后，同步${CACHE_NAME}缓存...")
            val userId = BeanKit.getProperty(any, UserContactWay::userId.name) as String
            CacheKit.evict(CACHE_NAME, getKey(userId))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserContactWayByUserIdCache>().getContactWays(userId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 联系方式id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的联系方式后，同步${CACHE_NAME}缓存...")
            val userId = BeanKit.getProperty(any, UserContactWay::userId.name) as String
            CacheKit.evict(CACHE_NAME, getKey(userId))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserContactWayByUserIdCache>().getContactWays(userId)
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 联系方式id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的联系方式的启用状态后，同步缓存...")
            val contactWay = userContactWayDao.get(id)
            if (contactWay == null) {
                log.warn("同步联系方式缓存时未找到id为${id}的记录。")
                return
            }
            val key = getKey(contactWay.userId ?: return)
            CacheKit.evict(CACHE_NAME, key)
            if (active && CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<UserContactWayByUserIdCache>().getContactWays(contactWay.userId!!)
            }
            log.debug("缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 联系方式id
     */
    open fun syncOnDelete(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val userId = BeanKit.getProperty(any, UserContactWay::userId.name) as String
            log.debug("删除id为${id}的联系方式后，同步从${CACHE_NAME}缓存中踢除...")
            CacheKit.evict(CACHE_NAME, getKey(userId))
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param ids 联系方式id集合
     * @param userIds 用户id集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>, userIds: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${ids}的联系方式后，同步从${CACHE_NAME}缓存中踢除...")
            userIds.distinct().forEach {
                CacheKit.evict(CACHE_NAME, getKey(it))
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param userId 用户ID
     * @return 缓存key
     */
    fun getKey(userId: String): String {
        return userId
    }

    private val log = LogFactory.getLog(this)

    //endregion your codes 2

}
