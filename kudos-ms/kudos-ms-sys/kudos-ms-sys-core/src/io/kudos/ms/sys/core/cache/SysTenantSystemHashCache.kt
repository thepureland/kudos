package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.tenant.SysTenantSystemCacheEntry
import io.kudos.ms.sys.core.cache.SysTenantSystemHashCache.Companion.CACHE_NAME
import io.kudos.ms.sys.core.cache.SysTenantSystemHashCache.Companion.FILTERABLE_PROPERTIES
import io.kudos.ms.sys.core.dao.SysTenantSystemDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component

/**
 * 租户-系统关系统一缓存处理器，基于 Hash 结构存储。
 *
 * 数据来源表：sys_tenant_system
 *
 * 提供按副属性查询与回写能力：
 *  1. 系统编码 -> Set<租户id>
 *  2. 租户id -> Set<系统编码>
 *
 * 使用 [FILTERABLE_PROPERTIES] 中的副属性建立 Set 索引，支持多条件等值查询；所有写入、删除、全量刷新均需使用同一副属性集合以保持索引一致。
 *
 * 使用前需在缓存配置表 sys_cache 中增加名为 [CACHE_NAME] 的配置项且 hash=true。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysTenantSystemHashCache : AbstractHashCacheHandler<SysTenantSystemCacheEntry>() {

    @Resource
    private lateinit var sysTenantSystemDao: SysTenantSystemDao

    private val log = LogFactory.getLog(this)

    companion object {
        const val CACHE_NAME = "SYS_TENANT_SYSTEM__HASH"

        /** 可筛选副属性，用于按 tenantId / systemCode 建二级索引 */
        val FILTERABLE_PROPERTIES = setOf(
            SysTenantSystemCacheEntry::tenantId.name,
            SysTenantSystemCacheEntry::systemCode.name
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysTenantSystemCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysTenantSystemCacheEntry? =
        sysTenantSystemDao.get(id.toString(), SysTenantSystemCacheEntry::class)

    // ---------- 按子系统编码 / 按租户id ----------

    /**
     * 按子系统编码获取租户id集合。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param systemCode 系统编码，非空
     * @return 该系统下的租户id集合
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#systemCode"],
        entityClass = SysTenantSystemCacheEntry::class,
        filterableProperties = ["tenantId", "systemCode"],
        returnProperty = "tenantId"
    )
    open fun getTenantIdsBySubSystemCode(systemCode: String): Set<String> {
        require(systemCode.isNotBlank()) { "按子系统编码查询时 systemCode 不能为空" }
        val list = sysTenantSystemDao.fetchCacheItemsBySystemCode(systemCode)
        if (list.isNotEmpty() && KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().saveBatch(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        }
        return list.map { it.tenantId }.toSet()
    }

    /**
     * 按租户id获取系统编码集合。
     * 先按副属性索引查缓存，未命中则查库并回写。
     *
     * @param tenantId 租户id，非空
     * @return 该租户下的系统编码集合
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#tenantId"],
        entityClass = SysTenantSystemCacheEntry::class,
        filterableProperties = ["tenantId", "systemCode"],
        returnProperty = "systemCode"
    )
    open fun getSubSystemCodesByTenantId(tenantId: String): Set<String> {
        require(tenantId.isNotBlank()) { "按租户id查询时 tenantId 不能为空" }
        val list = sysTenantSystemDao.fetchCacheItemsByTenantId(tenantId)
        if (list.isNotEmpty() && KeyValueCacheKit.isCacheActive(CACHE_NAME) && KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().saveBatch(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
        }
        return list.map { it.systemCode }.toSet()
    }

    // ---------- 全量刷新与同步 ----------

    /**
     * 从库全量加载租户-系统关系并刷新 Hash 缓存。
     *
     * @param clear 为 true 时先清空再写入；为 false 时覆盖写入
     */
    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载租户-系统关系 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysTenantSystemDao.fetchAllForCache()
        log.debug("从数据库加载 ${list.size} 条租户-系统关系，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增租户-系统关系后同步：将指定 id 的实体从库加载并写入缓存。
     *
     * @param id 主键
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysTenantSystemDao.get(id, SysTenantSystemCacheEntry::class) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增租户-系统关系后同步（重载，接收业务对象与 id）。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新租户-系统关系后同步：从库重新加载该 id 并写回缓存。
     *
     * @param id 主键
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysTenantSystemDao.get(id, SysTenantSystemCacheEntry::class) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新租户-系统关系后同步（重载）。
     *
     * @param any 业务对象，仅用于重载区分
     * @param id 主键
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * 删除租户-系统关系后同步：从缓存中移除该 id 及其副属性索引。
     *
     * @param id 主键
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysTenantSystemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除后同步：从缓存中移除这些 id 及其副属性索引。
     *
     * @param ids 主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        log.debug("批量删除 id 为 $ids 的 sys_tenant_system 后，同步从 ${cacheName()} 缓存中踢除...")
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysTenantSystemCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
        log.debug("${cacheName()} 缓存同步完成。")
    }
}
