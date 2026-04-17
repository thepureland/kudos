package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleCacheEntry
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * `sys_access_rule` 的 Hash 缓存：主键 id 存全文，副属性 `systemCode`、`tenantId`（空串表示平台级）建二级索引，
 * 支持按 id 与按「系统编码 + 租户」等值查询（与表唯一约束 `(system_code, tenant_id)` 一致）。
 *
 * 使用前须在 `sys_cache` 中配置 [CACHE_NAME] 且 `hash = true`。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysAccessRuleHashCache : AbstractHashCacheHandler<SysAccessRuleCacheEntry>() {

    @Resource
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val CACHE_NAME = "SYS_ACCESS_RULE__HASH"

        /** 副属性：按 systemCode、tenantId（空串 = 平台级）建 Set 索引 */
        val FILTERABLE_PROPERTIES = setOf(
            SysAccessRuleCacheEntry::systemCode.name,
            SysAccessRuleCacheEntry::tenantId.name,
        )
    }

    override fun cacheName(): String = CACHE_NAME

    override fun entityClass() = SysAccessRuleCacheEntry::class

    override fun filterableProperties(): Set<String> = FILTERABLE_PROPERTIES

    override fun doReload(id: Any): SysAccessRuleCacheEntry? =
        sysAccessRuleDao.fetchCacheEntryById(id.toString())

    /**
     * 按主键从缓存获取访问规则，未命中则查库并回写。
     *
     * @param id 主键，非空白
     * @return 缓存项；不存在时返回 `null`
     */
    @HashCacheableByPrimary(
        cacheNames = [CACHE_NAME],
        key = "#id",
        entityClass = SysAccessRuleCacheEntry::class,
        unless = "#result == null",
        filterableProperties = ["systemCode", "tenantId"],
    )
    open fun getAccessRuleById(id: String): SysAccessRuleCacheEntry? {
        require(id.isNotBlank()) { "按 id 获取访问规则时 id 不能为空" }
        return sysAccessRuleDao.fetchCacheEntryById(id)
    }

    /**
     * 按系统编码与租户ID从缓存获取访问规则，未命中则查库并回写。
     *
     * @param systemCode 系统编码（子系统），非空白
     * @param tenantId 租户 id；**空串**表示平台级（与库中 `tenant_id IS NULL` 对应），须与缓存副属性索引存值一致；调用方若持有可空租户 id，需先规范为 `trim().takeIf { it.isNotEmpty() } ?: ""`
     * @return 命中时返回该维度下唯一一行；不存在时返回 `null`
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#systemCode", "#tenantId"],
        entityClass = SysAccessRuleCacheEntry::class,
        filterableProperties = ["systemCode", "tenantId"],
    )
    open fun getAccessRuleBySystemCodeAndTenantId(systemCode: String, tenantId: String): SysAccessRuleCacheEntry? {
        require(systemCode.isNotBlank()) { "按系统编码获取访问规则时 systemCode 不能为空" }
        return sysAccessRuleDao.fetchCacheEntryBySystemCodeAndtenantId(systemCode, tenantId)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载访问规则 Hash 缓存")
            return
        }
        val cache = hashCache()
        if (clear) cache.clear(CACHE_NAME)
        val list = sysAccessRuleDao.fetchAllCacheEntries()
        log.debug("从数据库加载 ${list.size} 条访问规则，刷新 Hash 缓存")
        cache.refreshAll(CACHE_NAME, list, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增访问规则后同步写入 Hash 缓存。
     *
     * @param id 主键
     */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysAccessRuleDao.fetchCacheEntryById(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 新增访问规则后同步（与 [syncOnInsert] 同效，便于与其它 Cache 方法签名对齐）。
     *
     * @param any 业务入参占位
     * @param id 主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        syncOnInsert(id)
    }

    /**
     * 更新访问规则后按库中最新行回写 Hash 缓存。
     *
     * @param id 主键
     */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysAccessRuleDao.fetchCacheEntryById(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /**
     * 更新访问规则后同步（占位重载）。
     *
     * @param any 业务入参占位
     * @param id 主键
     */
    open fun syncOnUpdate(any: Any, id: String) {
        syncOnUpdate(id)
    }

    /**
     * 删除访问规则后从 Hash 缓存移除该 id 及副属性索引。
     *
     * @param id 主键
     */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysAccessRuleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /**
     * 批量删除后同步移除缓存。
     *
     * @param ids 主键集合
     */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysAccessRuleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
    }
}
