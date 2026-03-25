package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.core.cache.SysCacheHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.cache.SysCacheHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dao.SysCacheDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 缓存配置 Hash 缓存处理器，基于 Hash 结构存储 [SysCacheCacheEntry]。
 *
 * 数据来源表：sys_cache。
 *
 * 提供三类查询与回写能力：
 * - **按主键**：按 id 取单条或批量实体。
 * - **按原子服务编码+名称**：按 atomicServiceCode、name 取单条。
 * - **按原子服务编码**：按 atomicServiceCode 取列表。
 *
 * 使用 [FILTERABLE_PROPERTIES] 中的副属性建立 Set 索引；所有写入、删除、全量刷新均需使用同一副属性集合以保持索引一致。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysCacheHashCache : AbstractHashCacheHandler<SysCacheCacheEntry>() {

    @Resource
    private lateinit var sysCacheDao: SysCacheDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_CACHE__HASH"

        /** 用于等值筛选与 Set 索引的副属性名集合，写入/删除/全量刷新时须与此一致 */
        val FILTERABLE_PROPERTIES = setOf(
            SysCacheCacheEntry::name.name,
            SysCacheCacheEntry::atomicServiceCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysCacheCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysCacheCacheEntry? = sysCacheDao.getAs(id.toString())

    // ---------- 1. 按主键 id ----------

    /**
     * 根据主键 id 获取单条缓存配置。
     * 先查缓存，未命中则查库并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param id 缓存配置主键，非空
     * @return 缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysCacheCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCacheById(id: String): SysCacheCacheEntry? {
        require(id.isNotBlank()) { "获取缓存配置时 id 不能为空" }
        return sysCacheDao.getAs<SysCacheCacheEntry>(id)
    }

    /**
     * 根据主键 id 列表批量获取缓存配置。
     * 先查缓存，未命中的 id 再查库并回写；回写时按 [FILTERABLE_PROPERTIES] 建立副属性索引。
     *
     * @param ids 主键列表，可为空
     * @return id -> 实体 映射，仅包含能查到的 id
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysCacheCacheEntry::class,
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCachesByIds(ids: Set<String>): Map<String, SysCacheCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        val list = sysCacheDao.getByIdsAs<SysCacheCacheEntry>(ids)
        val byId = list.associateBy { it.id }
        return ids.mapNotNull { id -> byId[id]?.let { id to it } }.toMap()
    }

    // ---------- 2. 按原子服务编码+名称 ----------

    /**
     * 按原子服务编码、名称查询单条缓存配置。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @param name 缓存名称，非空
     * @return 匹配的缓存项，不存在时 null
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode", "#name"],
        entityClass = SysCacheCacheEntry::class,
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCache(atomicServiceCode: String, name: String): SysCacheCacheEntry? {
        require(atomicServiceCode.isNotBlank()) { "获取缓存配置时 atomicServiceCode 不能为空" }
        require(name.isNotBlank()) { "获取缓存配置时 name 不能为空" }
        return sysCacheDao.fetchCacheEntryByNameAndAtomicServiceCode(atomicServiceCode, name)
    }

    // ---------- 3. 按原子服务编码 ----------

    /**
     * 按原子服务编码查询，返回匹配的缓存配置列表。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param atomicServiceCode 原子服务编码，非空
     * @return 匹配的缓存项列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicServiceCode"],
        entityClass = SysCacheCacheEntry::class,
        filterableProperties = ["name", "atomicServiceCode"]
    )
    open fun getCaches(atomicServiceCode: String): List<SysCacheCacheEntry> {
        require(atomicServiceCode.isNotBlank()) { "获取缓存配置时 atomicServiceCode 不能为空" }
        return sysCacheDao.fetchCachesByAtomicServiceCode(atomicServiceCode)
    }

    // ---------- 全量刷新 ----------

    /**
     * 从库全量加载缓存配置并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空当前缓存再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载缓存配置 Hash 缓存")
            return
        }
        val cache = hashCache()
        val list = sysCacheDao.searchAs<SysCacheCacheEntry>()
        log.debug("从数据库加载 ${list.size} 条缓存配置，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        log.debug("缓存配置 Hash 缓存刷新完成")
    }

    // ---------- 写库后同步（供业务在新增/更新/删除后调用） ----------

    /**
     * 新增缓存配置后同步：将指定 id 的实体从库加载并写入缓存，并建立副属性索引。
     *
     * @param id 新增的缓存配置主键
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysCacheDao.getAs<SysCacheCacheEntry>(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增缓存配置后同步（重载，接收业务对象与 id）。行为同 [syncOnInsert(id)]。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 新增的缓存配置主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新缓存配置后同步：将指定 id 的实体从库重新加载并写入缓存，更新副属性索引。
     *
     * @param id 被更新的缓存配置主键
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysCacheDao.getAs<SysCacheCacheEntry>(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新缓存配置后同步（重载，带业务对象）。行为同 [syncOnUpdate(id)]。
     *
     * @param any 业务对象
     * @param id 被更新的缓存配置主键
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * 删除缓存配置后同步：从缓存中移除该 id，并从副属性 Set 索引中移除。
     *
     * @param id 被删除的缓存配置主键
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysCacheCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除缓存配置后同步：从缓存中移除这些 id，并从副属性 Set 索引中移除。
     *
     * @param ids 被删除的缓存配置主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        ids.forEach { cache.deleteById(CACHE_NAME, it, SysCacheCacheEntry::class, FILTERABLE_PROPERTIES, emptySet()) }
    }
}
