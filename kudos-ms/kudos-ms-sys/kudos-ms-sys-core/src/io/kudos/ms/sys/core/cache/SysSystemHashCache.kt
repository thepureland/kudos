package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ms.sys.core.cache.SysSystemHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.dao.SysSystemDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 系统（按 code）Hash 缓存处理器。
 *
 * 数据来源表：sys_system；主键为 code，缓存的 key 即 code，value 为 [SysSystemCacheItem]。
 * 使用 Hash 结构存储，通过 [HashCacheableByPrimary] / [HashBatchCacheableByPrimary] 按 code 存取。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysSystemHashCache : AbstractHashCacheHandler<SysSystemCacheItem>() {

    @Resource
    private lateinit var sysSystemDao: SysSystemDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "SYS_SYSTEM__HASH"

        /** 可筛选副属性：按 subSystem 建二级索引，用于按是否子系统查询 */
        val FILTERABLE_PROPERTIES = setOf(SysSystemCacheItem::subSystem.name)
    }

    override fun cacheName(): String = CACHE_NAME

    /**
     * 根据 code 从缓存获取系统信息，未命中则查库并回写。
     *
     * @param code 系统编码（主键），非空
     * @return 系统缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#code",
        entityClass = SysSystemCacheItem::class,
        unless = "#result == null",
        filterableProperties = ["subSystem"]
    )
    open fun getSystemByCode(code: String): SysSystemCacheItem? {
        require(code.isNotBlank()) { "获取系统时 code 不能为空" }
        return sysSystemDao.getCacheItem(code)
    }

    /**
     * 根据多个 code 从缓存批量获取系统信息，未命中的从库加载并回写。
     *
     * @param codes 系统 code 集合，可为空
     * @return code -> 实体 映射，仅包含能查到的 code
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysSystemCacheItem::class,
        filterableProperties = ["subSystem"]
    )
    open fun getSystemsByCodes(codes: List<String>): Map<String, SysSystemCacheItem> {
        if (codes.isEmpty()) return emptyMap()
        val list = sysSystemDao.listCacheItemsByIds(codes)
        return list.filter { it.id != null && it.id in codes }.associateBy { it.id!! }
    }

    /**
     * 根据是否子系统从缓存获取系统列表，未命中则查库并回写。
     *
     * @param subSystem true 表示只查子系统，false 表示只查非子系统
     * @return 系统缓存项列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#subSystem"],
        entityClass = SysSystemCacheItem::class,
        filterableProperties = ["subSystem"]
    )
    open fun getSystemsByType(subSystem: Boolean): List<SysSystemCacheItem> {
        return sysSystemDao.listCacheItemsBySubSystem(subSystem)
    }

    /** 获取所有子系统列表（subSystem=true）。 */
    open fun listSubSystems(): List<SysSystemCacheItem> = getSystemsByType(true)

    /**
     * 从库全量加载系统并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载系统 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.refreshAll(CACHE_NAME, emptyList<SysSystemCacheItem>(), FILTERABLE_PROPERTIES, emptySet())
        val list = sysSystemDao.listAllCacheItems()
        log.debug("从数据库加载 ${list.size} 条系统，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 新增系统后同步：将指定 code 的实体从库加载并写入缓存。 */
    open fun syncOnInsert(code: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME) || !CacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysSystemDao.getCacheItem(code) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 更新系统后同步：从库重新加载并写入缓存。 */
    open fun syncOnUpdate(code: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysSystemDao.getCacheItem(code) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** 删除系统后同步：从缓存中移除该 code。 */
    open fun syncOnDelete(code: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, code, SysSystemCacheItem::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 批量删除系统后同步：从缓存中移除这些 code。 */
    open fun syncOnBatchDelete(codes: Collection<String>) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        codes.forEach { cache.deleteById(CACHE_NAME, it, SysSystemCacheItem::class, FILTERABLE_PROPERTIES, emptySet()) }
    }
}
