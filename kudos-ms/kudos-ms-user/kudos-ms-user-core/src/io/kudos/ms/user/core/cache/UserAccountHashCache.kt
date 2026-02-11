package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.common.vo.user.UserAccountCacheItem
import io.kudos.ms.user.core.cache.UserAccountHashCache.Companion.CACHE_NAME
import io.kudos.ms.user.core.dao.UserAccountDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 用户 Hash 缓存处理器（整合原 UserByIdCache、UserIdByTenantIdAndUsernameCache 逻辑）
 *
 * 数据来源表：user_account；主键为 id，缓存的 key 即 id，value 为 [UserAccountCacheItem]。
 * 使用 Hash 结构存储，支持按 id 存取、按 tenantId+username 二级索引查询。
 *
 * - 按 id：getUserById、getUsersByIds（等价原 UserByIdCache，含 active=false）
 * - 按租户+用户名：getUsersByTenantIdAndUsername（等价原 UserIdByTenantIdAndUsernameCache 按租户+用户名查用户列表）
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class UserAccountHashCache : AbstractHashCacheHandler<UserAccountCacheItem>() {

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "USER_ACCOUNT__HASH"

        /** 可筛选副属性：按 tenantId、username 建二级索引 */
        val FILTERABLE_PROPERTIES = setOf(
            UserAccountCacheItem::tenantId.name,
            UserAccountCacheItem::username.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    /**
     * 根据 id 从缓存获取用户，未命中则查库并回写。
     *
     * @param id 用户 id（主键），非空
     * @return 用户缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = UserAccountCacheItem::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "username"]
    )
    open fun getUserById(id: String): UserAccountCacheItem? {
        require(id.isNotBlank()) { "获取用户时 id 不能为空" }
        return userAccountDao.getAs<UserAccountCacheItem>(id)
    }

    /**
     * 根据多个 id 从缓存批量获取用户，未命中的从库加载并回写。
     *
     * @param ids 用户 id 集合，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = UserAccountCacheItem::class,
        filterableProperties = ["tenantId", "username"]
    )
    open fun getUsersByIds(ids: Collection<String>): Map<String, UserAccountCacheItem> {
        if (ids.isEmpty()) return emptyMap()
        val list = userAccountDao.getByIdsAs<UserAccountCacheItem>(ids)
        return list.filter { it.id != null && it.id in ids }.associateBy { it.id!! }
    }

    /**
     * 根据租户、用户名从缓存获取用户列表，未命中则查库并回写。
     *
     * @param tenantId 租户 id
     * @param username 用户名
     * @return 用户缓存项，不存在返回null
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#username"],
        entityClass = UserAccountCacheItem::class,
        filterableProperties = ["tenantId", "username"]
    )
    open fun getUsersByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheItem? {
        return userAccountDao.getUsersByTenantIdAndUsername(tenantId, username)
    }

    /**
     * 从库全量加载用户并刷新 Hash 缓存（含 active=false，等价原 UserByIdCache 全量）。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载用户 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.refreshAll(CACHE_NAME, emptyList<UserAccountCacheItem>(), FILTERABLE_PROPERTIES, emptySet())
        val list = userAccountDao.searchAs<UserAccountCacheItem>()
        log.debug("从数据库加载 ${list.size} 条用户，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 新增用户后同步：将指定 id 的实体从库加载并写入缓存。 */
    open fun syncOnInsert(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME) || !CacheKit.isWriteInTime(CACHE_NAME)) return
        val item = userAccountDao.getAs<UserAccountCacheItem>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 更新用户后同步：从库重新加载并写入缓存。 */
    open fun syncOnUpdate(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = userAccountDao.getAs<UserAccountCacheItem>(id) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** 删除用户后同步：从缓存中移除该 id。 */
    open fun syncOnDelete(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, UserAccountCacheItem::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 批量删除用户后同步：从缓存中移除这些 id。 */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, UserAccountCacheItem::class, FILTERABLE_PROPERTIES, emptySet()) }
    }
}
