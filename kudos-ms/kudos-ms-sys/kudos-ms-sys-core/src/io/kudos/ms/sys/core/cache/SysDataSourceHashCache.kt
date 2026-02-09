package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.core.cache.SysDataSourceHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.dao.SysDataSourceDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 数据源统一 Hash 缓存处理器，整合按 id 与按「租户id+子系统编码+微服务编码」两类查询。
 *
 * 1. 数据来源表：sys_data_source
 * 2. 按主键 id 存取：缓存所有数据源（含 active=false）
 * 3. 按租户id+子系统编码+微服务编码： tenantId 非空的记录
 * 4. 使用 Hash 结构，主键为 id；副属性索引：tenantId、subSystemCode、microServiceCode
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysDataSourceHashCache : AbstractHashCacheHandler<SysDataSourceCacheItem>() {

    @Resource
    private lateinit var sysDataSourceDao: SysDataSourceDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "SYS_DATA_SOURCE__HASH"

        /** 可筛选副属性，用于按 tenantId/subSystemCode/microServiceCode 建二级索引 */
        val FILTERABLE_PROPERTIES = setOf(
            SysDataSourceCacheItem::tenantId.name,
            SysDataSourceCacheItem::subSystemCode.name,
            SysDataSourceCacheItem::microServiceCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    // ---------- 按主键 id（与 DataSourceByIdCache 等价） ----------

    /**
     * 根据 id 从缓存获取数据源，未命中则查库并回写。
     *
     * @param id 数据源主键，非空
     * @return 数据源缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysDataSourceCacheItem::class,
        unless = "#result == null",
        filterableProperties = ["tenantId", "subSystemCode", "microServiceCode"]
    )
    open fun getDataSourceById(id: String): SysDataSourceCacheItem? {
        require(id.isNotBlank()) { "获取数据源时 id 不能为空" }
        return sysDataSourceDao.get(id, SysDataSourceCacheItem::class)
    }

    /**
     * 根据多个 id 批量从缓存获取数据源，未命中的从库加载并回写。
     *
     * @param ids 数据源 id 集合，可为空
     * @return id -> 缓存对象 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysDataSourceCacheItem::class,
        filterableProperties = ["tenantId", "subSystemCode", "microServiceCode"]
    )
    open fun getDataSourcesByIds(ids: List<String>): Map<String, SysDataSourceCacheItem> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysDataSourceDao.fetchDataSourcesByIdsForCache(ids)
        return list.filter { it.id != null && it.id in ids }.associateBy { it.id!! }
    }

    // ---------- 按租户id+子系统编码+微服务编码（与 DataSourceByTenantIdAnd3CodesCache 等价） ----------

    /**
     * 按租户id、子系统编码、微服务编码多条件等值查询，返回匹配的数据源列表
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param tenantId 租户 id，非空
     * @param subSystemCode 子系统编码, 可为null
     * @param microServiceCode 微服务编码，可为 null
     * @return 匹配的缓存列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId", "#subSystemCode", "#microServiceCode"],
        entityClass = SysDataSourceCacheItem::class,
        filterableProperties = ["tenantId", "subSystemCode", "microServiceCode"]
    )
    open fun getDataSources(
        tenantId: String,
        subSystemCode: String?,
        microServiceCode: String?
    ): List<SysDataSourceCacheItem> {
        require(tenantId.isNotBlank()) { "获取数据源时租户ID必须指定" }
        return sysDataSourceDao.fetchDataSourcesForCache(tenantId, subSystemCode, microServiceCode)
    }


    // ---------- 全量刷新与同步 ----------

    /**
     * 从库全量加载数据源并刷新 Hash 缓存（含所有记录，与按 id 缓存一致）。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载数据源 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.refreshAll(CACHE_NAME, emptyList<SysSystemCacheItem>(), FILTERABLE_PROPERTIES, emptySet())
        val list = sysDataSourceDao.searchAs<SysDataSourceCacheItem>()
        log.debug("从数据库加载 ${list.size} 条数据源，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增数据源后同步：将指定 id 的实体从库加载并写入缓存。
     *
     * @param any 包含必要属性的对象（用于判断是否需写 3-codes 索引）
     * @param id 数据源 id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME) || !CacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysDataSourceDao.get(id, SysDataSourceCacheItem::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 更新数据源后同步：先按旧 key 从索引移除，再按新数据写回。
     *
     * @param any 包含必要属性的对象
     * @param id 数据源 id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysDataSourceDao.get(id, SysDataSourceCacheItem::class) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新启用状态后同步：从库重新加载该 id 并写回缓存（索引会随之更新）。
     *
     * @param id 数据源 id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysDataSourceDao.get<SysDataSourceCacheItem>(id) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 删除数据源后同步：从缓存中移除该 id 及其副属性索引。
     *
     * @param id 数据源 id
     */
    open fun syncOnDelete(id: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysDataSourceCacheItem::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除数据库对象后，同步缓存
     *
     * @param ids 主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (CacheKit.isCacheActive(cacheName())) {
            log.debug("批量删除id为${ids}的sys_data_source后，同步从${cacheName()}缓存中踢除...")
            //TODO batch
            ids.forEach {
                CacheKit.evict(cacheName(), it) // 踢除角色缓存
            }
            log.debug("${cacheName()}缓存同步完成。")
        }
    }

}
