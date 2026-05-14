package io.kudos.ms.sys.core.accessrule.cache

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.core.hash.AbstractHashCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleCacheEntry
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleUpdated
import jakarta.annotation.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * `sys_access_rule` 的 Hash 缓存：主键 id 存全文，副属性 `systemCode`、`tenantId` 建二级索引，
 * 支持按 id 与按「系统编码 + 租户」等值查询（与表唯一约束 `(system_code, tenant_id)` 一致）。
 *
 * 租户维度归一化由 [AccessRuleTenantKey.normalize] 统一处理：`null` / 空白一律映射为空串（平台级），
 * 与 [AccessRuleIpsBySubSysAndTenantIdCache] 的 KV key 取值保持一致。
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
     * @param tenantId 租户 id；`null` / 空白一律视为平台级（与库中 `tenant_id IS NULL` 对应）。
     *                 内部由 [AccessRuleTenantKey.normalize] 归一为空串后参与副属性索引匹配。
     * @return 命中时返回该维度下唯一一行；不存在时返回 `null`
     */
    open fun getAccessRuleBySystemCodeAndTenantId(systemCode: String, tenantId: String?): SysAccessRuleCacheEntry? {
        require(systemCode.isNotBlank()) { "按系统编码获取访问规则时 systemCode 不能为空" }
        // 通过 self-proxy 调用，确保 @HashCacheableBySecondary AOP 生效；
        // tenantKey 在入口处统一归一化，避免 filterExpressions 出现表达式（框架要求单参数 SpEL）。
        return getSelf<SysAccessRuleHashCache>()
            .findBySystemCodeAndTenantKey(systemCode, AccessRuleTenantKey.normalize(tenantId))
    }

    /**
     * 内部查询入口，仅供 [getAccessRuleBySystemCodeAndTenantId] 调用：
     * 入参 [tenantKey] **必须已归一化**（`null` / 空白先转空串），供 `@HashCacheableBySecondary` 的副属性索引匹配。
     */
    @HashCacheableBySecondary(
        cacheNames = [CACHE_NAME],
        filterExpressions = ["#systemCode", "#tenantKey"],
        entityClass = SysAccessRuleCacheEntry::class,
        filterableProperties = ["systemCode", "tenantId"],
    )
    open fun findBySystemCodeAndTenantKey(systemCode: String, tenantKey: String): SysAccessRuleCacheEntry? =
        sysAccessRuleDao.fetchCacheEntryBySystemCodeAndTenantId(systemCode, tenantKey)

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

    // region 事件订阅（由 SysAccessRuleService 在事务提交后派发） -------------------------------------

    /** 新增完成后写入 Hash 缓存。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleInserted): Unit = syncOnInsert(event.id)

    /** 更新完成后按库中最新行回写。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleUpdated): Unit = syncOnUpdate(event.id)

    /** 删除完成后清缓存。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleDeleted): Unit = syncOnDelete(event.id)

    /** 批量删除完成后逐条清缓存。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysAccessRuleBatchDeleted): Unit = syncOnBatchDelete(event.ids)

    // endregion

    // region 同步原语（保留为 internal 助手，供事件 listener 与现有测试直接调用） ----------------

    /** 按主键回写 Hash 缓存。 */
    open fun syncOnInsert(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME) || !KeyValueCacheKit.isWriteInTime(CACHE_NAME)) return
        val item = sysAccessRuleDao.fetchCacheEntryById(id) ?: return
        hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 按主键重新加载并回写。 */
    open fun syncOnUpdate(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val item = sysAccessRuleDao.fetchCacheEntryById(id) ?: return
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            hashCache().save(CACHE_NAME, item, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    /** 按主键移除条目与副属性索引。 */
    open fun syncOnDelete(id: String) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        hashCache().deleteById(CACHE_NAME, id, SysAccessRuleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
    }

    /** 批量移除。 */
    open fun syncOnBatchDelete(ids: Collection<String>) {
        if (!KeyValueCacheKit.isCacheActive(cacheName())) return
        val cache = hashCache()
        ids.forEach {
            cache.deleteById(cacheName(), it, SysAccessRuleCacheEntry::class, FILTERABLE_PROPERTIES, emptySet())
        }
    }

    // endregion
}
