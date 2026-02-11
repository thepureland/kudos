package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ms.sys.core.cache.SysMicroServiceHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.dao.SysMicroServiceDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 微服务（按 code）Hash 缓存处理器
 *
 * 数据来源表：sys_micro_service；主键为 code，缓存的 key 即 code，value 为 [SysMicroServiceCacheItem]。
 * 使用 Hash 结构存储，通过 [HashCacheableByPrimary] / [HashBatchCacheableByPrimary] 按 code 存取。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysMicroServiceHashCache : AbstractHashCacheHandler<SysMicroServiceCacheItem>() {

    @Resource
    private lateinit var sysMicroServiceDao: SysMicroServiceDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "SYS_MICRO_SERVICE__HASH"

        /** 可筛选副属性：按 atomicService 建二级索引，用于按是否原子服务查询 */
        val FILTERABLE_PROPERTIES = setOf(SysMicroServiceCacheItem::atomicService.name)
    }

    override fun cacheName(): String = CACHE_NAME

    /**
     * 根据 code 从缓存获取微服务信息，未命中则查库并回写。
     *
     * @param code 微服务编码（主键），非空
     * @return 微服务缓存项，不存在时 null
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#code",
        entityClass = SysMicroServiceCacheItem::class,
        unless = "#result == null",
        filterableProperties = ["atomicService"]
    )
    open fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem? {
        require(code.isNotBlank()) { "获取微服务时 code 不能为空" }
        return sysMicroServiceDao.get(code, SysMicroServiceCacheItem::class)
    }

    /**
     * 根据多个 code 从缓存批量获取微服务信息，未命中的从库加载并回写。
     *
     * @param codes 微服务 code 集合，可为空
     * @return code -> 实体 映射，仅包含能查到的 code
     */
    @HashBatchCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        entityClass = SysMicroServiceCacheItem::class,
        filterableProperties = ["atomicService"]
    )
    open fun getMicroServicesByCodes(codes: List<String>): Map<String, SysMicroServiceCacheItem> {
        if (codes.isEmpty()) return emptyMap()
        val list = sysMicroServiceDao.getByIdsAs<SysMicroServiceCacheItem>(codes)
        return list.filter { it.id != null && it.id in codes }.associateBy { it.id!! }
    }

    /**
     * 根据是否原子服务从缓存获取微服务列表，未命中则查库并回写。
     *
     * @param atomicService true 表示只查原子服务，false 表示只查非原子服务
     * @return 微服务缓存项列表
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#atomicService"],
        entityClass = SysMicroServiceCacheItem::class,
        filterableProperties = ["atomicService"]
    )
    open fun getMicroServicesByType(atomicService: Boolean): List<SysMicroServiceCacheItem> {
        return sysMicroServiceDao.fetchMicroServiceByTypeForCache(atomicService)
    }

    /** 获取所有原子服务列表（atomicService=true）。 */
    open fun listAtomicServices(): List<SysMicroServiceCacheItem> = getMicroServicesByType(true)

    /**
     * 从库全量加载微服务并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载微服务 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.refreshAll(CACHE_NAME, emptyList<SysMicroServiceCacheItem>(), FILTERABLE_PROPERTIES, emptySet())
        val list = sysMicroServiceDao.searchAs<SysMicroServiceCacheItem>()
        log.debug("从数据库加载 ${list.size} 条微服务，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 新增微服务后同步：将指定 code 的实体从库加载并写入缓存。 */
    open fun syncOnInsert(code: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME) || !CacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysMicroServiceDao.get(code, SysMicroServiceCacheItem::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 更新微服务后同步：从库重新加载并写入缓存。 */
    open fun syncOnUpdate(code: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysMicroServiceDao.get(code, SysMicroServiceCacheItem::class) ?: return
        if (CacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** 删除微服务后同步：从缓存中移除该 code。 */
    open fun syncOnDelete(code: String) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, code, SysMicroServiceCacheItem::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 批量删除微服务后同步：从缓存中移除这些 code。 */
    open fun syncOnBatchDelete(codes: Collection<String>) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) return
        val cache = hashCache()
        codes.forEach { cache.deleteById(CACHE_NAME, it, SysMicroServiceCacheItem::class, FILTERABLE_PROPERTIES, emptySet()) }
    }
}
