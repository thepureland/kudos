package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.common.vo.org.UserOrgCacheItem
import io.kudos.ms.user.core.cache.UserOrgHashCache.Companion.CACHE_NAME
import io.kudos.ms.user.core.dao.UserOrgDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 机构 Hash 缓存处理器
 *
 * 数据来源表：user_org；主键为 id，缓存的 key 即 id，value 为 [UserOrgCacheItem]。
 * 使用 Hash 结构存储，支持按 id 存取、按 tenantId 二级索引查询。
 *
 * - 按 id：getOrgById、getOrgsByIds（等价原 OrgByIdCache，含 active=false）
 * - 按租户：getOrgsByTenantId、getOrgIdsByTenantId
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class UserOrgHashCache : AbstractHashCacheHandler<UserOrgCacheItem>() {

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "USER_ORG__HASH"

        /** 可筛选副属性：按 tenantId 索引，用于按租户查询 */
        val FILTERABLE_PROPERTIES = setOf(
            UserOrgCacheItem::tenantId.name,
        )
    }

    override fun cacheName(): String = CACHE_NAME

    /**
     * 根据 id 从缓存获取机构，未命中则查库并回写。
     *
     * @param id 机构 id（主键），非空
     * @return 机构缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = UserOrgCacheItem::class,
        unless = "#result == null",
        filterableProperties = ["tenantId"]
    )
    open fun getOrgById(id: String): UserOrgCacheItem? {
        require(id.isNotBlank()) { "获取机构时 id 不能为空" }
        return userOrgDao.getCacheItem(id)
    }

    /**
     * 根据多个 id 从缓存批量获取机构，未命中的从库加载并回写。
     *
     * @param ids 机构 id 集合，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = UserOrgCacheItem::class,
        filterableProperties = ["tenantId"]
    )
    open fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheItem> {
        if (ids.isEmpty()) return emptyMap()
        val list = userOrgDao.getOrgsByIdsForCache(ids)
        return list.filter { it.id != null && it.id in ids }.associateBy { it.id!! }
    }

    /**
     * 根据租户ID从缓存获取机构列表，未命中则查库并回写。
     *
     * @param tenantId 租户 id
     * @return 机构缓存项列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#active"],
        entityClass = UserOrgCacheItem::class,
        filterableProperties = ["tenantId"]
    )
    open fun getOrgsByTenantId(tenantId: String): List<UserOrgCacheItem> {
        return userOrgDao.getOrgsByTenantIdForCache(tenantId)
    }

    /**
     * 从库全量加载机构并刷新 Hash 缓存（含 active=false，等价原 OrgByIdCache 全量）。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载机构 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.refreshAll(CACHE_NAME, emptyList<UserOrgCacheItem>(), FILTERABLE_PROPERTIES, emptySet())
        val list = userOrgDao.getAllOrgsForCache()
        log.debug("从数据库加载 ${list.size} 条机构，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 新增机构后同步：将指定 id 的实体从库加载并写入缓存。 */
    open fun syncOnInsert(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME) || !CacheKit.isWriteInTime(CACHE_NAME)) return
        val item = userOrgDao.getCacheItem(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 更新机构后同步：从库重新加载并写入缓存。 */
    open fun syncOnUpdate(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = userOrgDao.getCacheItem(id) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** 删除机构后同步：从缓存中移除该 id。 */
    open fun syncOnDelete(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, UserOrgCacheItem::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 批量删除机构后同步：从缓存中移除这些 id。 */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, UserOrgCacheItem::class, FILTERABLE_PROPERTIES, emptySet()) }
    }
}
