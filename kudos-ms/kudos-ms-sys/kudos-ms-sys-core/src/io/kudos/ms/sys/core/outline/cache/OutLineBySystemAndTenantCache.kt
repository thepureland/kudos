package io.kudos.ms.sys.core.outline.cache

import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.isNull
import io.kudos.context.support.Consts
import io.kudos.ms.sys.common.outline.vo.SysOutLineCacheEntry
import io.kudos.ms.sys.core.outline.dao.SysOutLineDao
import io.kudos.ms.sys.core.outline.event.SysOutLineBatchDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineDeleted
import io.kudos.ms.sys.core.outline.event.SysOutLineInserted
import io.kudos.ms.sys.core.outline.event.SysOutLineUpdated
import io.kudos.ms.sys.core.outline.model.po.SysOutLine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener


/**
 * 出网白名单缓存处理器
 *
 * - 数据来源表：`sys_out_line`
 * - 仅缓存 `active=true` 的规则
 * - 缓存 key 形式：`systemCode::归一化 tenantId`（参见 [OutLineSystemTenantKey]）
 * - 缓存 value：`List<SysOutLineCacheEntry>`
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class OutLineBySystemAndTenantCache : AbstractKeyValueCacheHandler<List<SysOutLineCacheEntry>>() {

    @Autowired
    private lateinit var dao: SysOutLineDao

    companion object {
        private const val CACHE_NAME = "SYS_OUT_LINE_BY_SYSTEM_AND_TENANT"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<SysOutLineCacheEntry> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式非法!"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER, limit = 2)
        val tenantId = parts.getOrNull(1)?.takeIf { it.isNotEmpty() }
        return getSelf<OutLineBySystemAndTenantCache>().listOutLines(parts[0], tenantId)
    }

    override fun reloadAll(clear: Boolean) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载出网白名单！")
            return
        }
        val all = dao.searchAs<SysOutLineCacheEntry>(Criteria(SysOutLine::active eq true))
        log.debug("从数据库加载了${all.size}条出网白名单。")
        if (clear) clear()
        val grouped = all.groupBy { OutLineSystemTenantKey.compositeKey(it.systemCode, it.tenantId) }
        grouped.forEach { (k, list) -> KeyValueCacheKit.put(CACHE_NAME, k, list) }
        log.debug("出网白名单缓存写入 ${grouped.size} 个维度。")
    }

    /**
     * 按 (systemCode, tenantId) 取启用的出网白名单。`tenantId == null` 表示平台级。
     *
     * @param systemCode 系统编码
     * @param tenantId 租户 id；`null` / 空白视为平台级
     * @return 缓存项列表；查无结果返回空列表
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#systemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat((#tenantId ?: '').trim())",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun listOutLines(systemCode: String, tenantId: String? = null): List<SysOutLineCacheEntry> {
        if (KeyValueCacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在 systemCode=${systemCode} 且 tenantId=${tenantId} 的出网白名单，从数据库中加载...")
        }
        require(systemCode.isNotBlank()) { "获取出网白名单时 systemCode 不能为空" }
        val tenantCriterion = if (tenantId == null) {
            SysOutLine::tenantId.isNull()
        } else {
            SysOutLine::tenantId eq tenantId
        }
        val criteria = Criteria(SysOutLine::systemCode eq systemCode)
            .addAnd(tenantCriterion)
            .addAnd(SysOutLine::active eq true)
        return dao.searchAs(criteria)
    }

    /** 失效指定 (systemCode, tenantId) 维度并按需回填 */
    open fun refreshDimension(systemCode: String, tenantId: String?) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val key = OutLineSystemTenantKey.compositeKey(systemCode, tenantId)
        KeyValueCacheKit.evict(CACHE_NAME, key)
        if (KeyValueCacheKit.isWriteInTime(CACHE_NAME)) {
            getSelf<OutLineBySystemAndTenantCache>().listOutLines(systemCode, tenantId)
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineInserted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        val po = dao.get(event.id) ?: return
        refreshDimension(po.systemCode, po.tenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineUpdated) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        // 更新后行仍可查；如发生跨维度迁移则旧维度缓存可能轻微滞后到下次启动 reloadAll 时纠正
        val po = dao.get(event.id) ?: return
        refreshDimension(po.systemCode, po.tenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineDeleted) {
        refreshDimension(event.systemCode, event.tenantId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: SysOutLineBatchDeleted) {
        if (!KeyValueCacheKit.isCacheActive(CACHE_NAME)) return
        event.dimensions.forEach { (systemCode, tenantId) -> refreshDimension(systemCode, tenantId) }
    }

    private val log = LogFactory.getLog(this::class)

}
