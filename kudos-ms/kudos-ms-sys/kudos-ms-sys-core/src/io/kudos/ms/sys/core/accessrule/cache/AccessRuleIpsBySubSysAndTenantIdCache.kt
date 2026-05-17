package io.kudos.ms.sys.core.accessrule.cache
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleIpCacheEntry
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleIpQuery
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.accessrule.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpBatchDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpDeleted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpInserted
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleIpUpdated
import io.kudos.ms.sys.core.accessrule.event.SysAccessRuleUpdated
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * ip 访问规则缓存处理器
 *
 * - 数据来源表：`sys_access_rule` & `sys_access_rule_ip`
 * - 仅缓存 `active=true` 的规则
 * - 缓存 key 形式：`系统编码::归一化租户id`。「归一化」即由 [AccessRuleTenantKey.normalize] 把 `null` / 空白统一转为空串，
 *   与 [SysAccessRuleHashCache] 中副属性索引取值保持一致。
 * - 缓存 value：[SysAccessRuleIpCacheEntry] 列表
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AccessRuleIpsBySubSysAndTenantIdCache : AbstractKeyValueCacheHandler<List<SysAccessRuleIpCacheEntry>>() {

    @Autowired
    private lateinit var sysAccessRuleIpDao: SysAccessRuleIpDao

    @Autowired
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    companion object Companion {
        private const val CACHE_NAME = "SYS_ACCESS_RULE_IPS_BY_SYSTEM_CODE_AND_TENANT_ID"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<SysAccessRuleIpCacheEntry> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式非法!"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER, limit = 2)
        // 已归一化：空串即平台级，对应库中 `tenant_id IS NULL`；查询层仍以 null 表达
        val tenantId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(parts[0], tenantId)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的ip访问规则！")
            return
        }

        // 关联查询
        val searchPayload = SysAccessRuleIpQuery(
            active = true,
            parentRuleActive = true
        )
        val results = sysAccessRuleIpDao.pagingSearch(searchPayload)

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存数据
        val ipRulesMap = results.mapNotNull { record ->
            val systemCode = record.systemCode ?: return@mapNotNull null
            AccessRuleTenantKey.compositeKey(systemCode, record.tenantId) to record
        }.groupBy({ it.first }, { it.second })
        ipRulesMap.forEach { (key, ipRules) ->
            val cacheItems = mapToCacheItems(ipRules)
            KeyValueCacheKit.put(CACHE_NAME, key, cacheItems)
        }
        log.debug("缓存了ip访问规则共${results.size}条。")
    }

    /**
     * 从缓存中获取 ip 访问规则，未命中则查库回填。
     *
     * 缓存 key 形式：`系统编码::归一化租户id`。`null` / 空白一律归一为空串（平台级），与库中 `tenant_id IS NULL` 对应。
     *
     * @param systemCode 系统编码
     * @param tenantId 租户 id，传 `null`（或空白）表示平台级规则
     * @return IP 缓存项列表，未匹配时返回空列表
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#systemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat((#tenantId ?: '').trim())",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getAccessRuleIps(systemCode: String, tenantId: String? = null): List<SysAccessRuleIpCacheEntry> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug(
                "${CACHE_NAME}缓存中不存在系统编码为${systemCode}且租户id为${tenantId}的ip访问规则，从数据库中加载..."
            )
        }
        require(systemCode.isNotBlank()) { "获取ip访问规则时，系统代码必须指定！" }
        val searchPayload = SysAccessRuleIpQuery(
            active = true,
            parentRuleActive = true,
            systemCode = systemCode,
            tenantId = tenantId,
            explicitNullProperties = if (tenantId == null) {
                listOf(SysAccessRule::tenantId.name)
            } else {
                null
            },
        )

        val results = sysAccessRuleIpDao.pagingSearch(searchPayload)
        return if (results.isEmpty()) {
            log.warn("数据库中找不到租户id为${tenantId}且系统编码为${systemCode}的ip访问规则！")
            listOf()
        } else {
            mapToCacheItems(results)
        }
    }

    // region 事件订阅：父规则（sys_access_rule）变更时联动 IP 缓存 ----------------------------------

    /** 父规则新增：刷新对应维度（此时尚无 IP，相当于建空槽位）。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentInserted(event: SysAccessRuleInserted): Unit =
        syncOnDeleteBySystemAndTenant(event.systemCode, event.tenantId)

    /** 父规则更新：刷新新维度；若维度迁移则同时刷新旧维度。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentUpdated(event: SysAccessRuleUpdated) {
        syncOnDeleteBySystemAndTenant(event.systemCode, event.tenantId)
        if (event.dimensionChanged) {
            syncOnDeleteBySystemAndTenant(event.beforeSystemCode!!, event.beforeTenantId)
        }
    }

    /** 父规则删除：刷新对应维度（清掉残留 IP 缓存）。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentDeleted(event: SysAccessRuleDeleted): Unit =
        syncOnDeleteBySystemAndTenant(event.systemCode, event.tenantId)

    /** 父规则批量删除：按事件附带的所有维度键逐一刷新。 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onParentBatchDeleted(event: SysAccessRuleBatchDeleted) {
        event.dimensions.forEach { (sysCode, tid) -> syncOnDeleteBySystemAndTenant(sysCode, tid) }
    }

    // endregion

    // region 事件订阅：IP 规则自身变更 -----------------------------------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpInserted(event: SysAccessRuleIpInserted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        if (!event.active) {
            log.debug("新增 IP 规则 ${event.id} active=false，跳过 ${CACHE_NAME} 同步。")
            return
        }
        refreshDimension(event.parentSystemCode, event.parentTenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpUpdated(event: SysAccessRuleIpUpdated): Unit =
        refreshDimension(event.parentSystemCode, event.parentTenantId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpDeleted(event: SysAccessRuleIpDeleted): Unit =
        refreshDimension(event.parentSystemCode, event.parentTenantId)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun onIpBatchDeleted(event: SysAccessRuleIpBatchDeleted) {
        event.dimensions.forEach { (sysCode, tid) -> refreshDimension(sysCode, tid) }
    }

    // endregion

    private fun refreshDimension(systemCode: String, tenantId: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        KeyValueCacheKit.evict(CACHE_NAME, getKey(systemCode, tenantId))
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(systemCode, tenantId)
        }
    }

    /**
     * 依据系统编码与租户维度删除并回填缓存，适用于删除前已拿到维度键的场景。
     */
    open fun syncOnDeleteBySystemAndTenant(systemCode: String, tenantId: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val cacheKey = getKey(systemCode, tenantId)
        KeyValueCacheKit.evict(CACHE_NAME, cacheKey)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(systemCode, tenantId)
        }
    }

    private fun mapToCacheItems(ruleIpRecords: List<SysAccessRuleIpRow>): List<SysAccessRuleIpCacheEntry> {
        return ruleIpRecords.map {
            SysAccessRuleIpCacheEntry(
                id = it.id,
                ipStart = it.ipStart,
                ipEnd = it.ipEnd,
                ipTypeDictCode = it.ipTypeDictCode,
                expirationTime = it.expirationTime
            )
        }
    }

    /**
     * 返回参数拼接后的 key。`null` / 空白一律归一为空串（平台级），与 [@Cacheable] 中的 SpEL 表达式保持一致。
     *
     * @param systemCode 系统编码
     * @param tenantId 租户 id；`null` / 空白视为平台级
     * @return 缓存 key
     */
    open fun getKey(systemCode: String, tenantId: String? = null): String =
        AccessRuleTenantKey.compositeKey(systemCode, tenantId)

    private val log = LogFactory.getLog(this::class)

}
