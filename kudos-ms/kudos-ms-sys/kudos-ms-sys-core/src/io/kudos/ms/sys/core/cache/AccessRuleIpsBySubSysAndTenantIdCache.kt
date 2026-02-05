package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.core.keyvalue.AbstractKeyValueCacheHandler
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpCacheItem
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpRecord
import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpSearchPayload
import io.kudos.ms.sys.core.dao.SysAccessRuleDao
import io.kudos.ms.sys.core.dao.SysAccessRuleIpDao
import io.kudos.ms.sys.core.model.po.SysAccessRule
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * ip访问规则缓存处理器
 *
 * 1.数据来源表：sys_access_rule & sys_access_rule_ip
 * 2.仅缓存active=true的
 * 3.缓存key为：系统编码::租户id，租户id有可能为null
 * 4.缓存value为：SysAccessRuleIpCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AccessRuleIpsBySubSysAndTenantIdCache : AbstractKeyValueCacheHandler<List<SysAccessRuleIpCacheItem>>() {

    @Autowired
    private lateinit var sysAccessRuleIpDao: SysAccessRuleIpDao

    @Autowired
    private lateinit var sysAccessRuleDao: SysAccessRuleDao

    companion object Companion {
        private const val CACHE_NAME = "SYS_ACCESS_RULE_IPS_BY_SYSTEM_CODE_AND_TENANT_ID"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): List<SysAccessRuleIpCacheItem> {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式非法!"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(parts[0], parts[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的ip访问规则！")
            return
        }

        // 关联查询
        val searchPayload = SysAccessRuleIpSearchPayload().apply {
            active = true
            parentRuleActive = true
        }
        val results = sysAccessRuleIpDao.pagingSearch(searchPayload)

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存数据
        val ipRulesMap = results.groupBy { getKey(it.systemCode!!, it.tenantId) }
        ipRulesMap.forEach { (key, ipRules) ->
            val cacheItems = mapToCacheItems(ipRules)
            CacheKit.put(CACHE_NAME, key, cacheItems)
        }
        log.debug("缓存了ip访问规则共${results.size}条。")
    }

    /**
     * 从缓存中获取ip访问规则，如果缓存中没有，从数据库中加载，并写入缓存.
     *
     * 缓存的key形式为：系统编码::租户id，租户id有可能为null
     *
     * @param systemCode 系统编码
     * @param tenantId 租户id，可以为null
     * @return SysAccessRuleIpCacheItem，不存在时返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#systemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#tenantId ?: 'null')",
        unless = "#result == null"
    )
    open fun getAccessRuleIps(systemCode: String, tenantId: String? = null): List<SysAccessRuleIpCacheItem> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug(
                "${CACHE_NAME}缓存中不存在系统编码为${systemCode}且租户id为${tenantId}的ip访问规则，从数据库中加载..."
            )
        }
        require(systemCode.isNotBlank()) { "获取ip访问规则时，系统代码必须指定！" }
        val searchPayload = SysAccessRuleIpSearchPayload().apply {
            active = true
            parentRuleActive = true
            this.systemCode = systemCode
            this.tenantId = tenantId
            nullProperties = listOf(this::tenantId.name)
        }

        val results = sysAccessRuleIpDao.pagingSearch(searchPayload)
        return if (results.isEmpty()) {
            log.warn("数据库中找不到租户id为${tenantId}且系统编码为${systemCode}的ip访问规则！")
            listOf()
        } else {
            mapToCacheItems(results)
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param ipRuleId ip访问规则id
     */
    open fun syncOnInsert(any: Any, ipRuleId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${ipRuleId}的ip访问规则后，同步${CACHE_NAME}缓存...")

            val props = BeanKit.extract(any)
            val systemCode = props[SysAccessRule::systemCode.name] as String
            val tenantId = props[SysAccessRule::tenantId.name] as String?

            // 踢除ip访问规则缓存
            CacheKit.evict(CACHE_NAME, getKey(systemCode, tenantId))

            val active = props[SysAccessRule::active.name] as Boolean?
            if (CacheKit.isWriteInTime(CACHE_NAME) && (active == null || active)) {
                // 缓存
                getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(systemCode, tenantId)
                log.debug("${CACHE_NAME}缓存同步完成。")
            } else {
                log.debug("新增的ip访问规则的active为false，不需要同步${CACHE_NAME}缓存。")
            }
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id ip访问规则id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的ip访问规则后，同步${CACHE_NAME}缓存...")
            val props = BeanKit.extract(any)
            val systemCode = props[SysAccessRule::systemCode.name] as String
            val tenantId = props[SysAccessRule::tenantId.name] as String?

            // 踢除ip访问规则缓存
            CacheKit.evict(CACHE_NAME, getKey(systemCode, tenantId))

            // 重新缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                val active = props[SysAccessRule::active.name] as Boolean?
                if (active == null || active) {
                    getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(systemCode, tenantId)
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param ipRuleId ip访问规则id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(ipRuleId: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${ipRuleId}的ip访问规则的启用状态后，同步${CACHE_NAME}缓存...")
            val sysAccessRuleIp = sysAccessRuleIpDao.get(ipRuleId)
            if (sysAccessRuleIp == null) {
                log.error("数据库中找不到id为${ipRuleId}的ip访问规则！")
                return
            }

            val sysAccessRule = sysAccessRuleDao.get(sysAccessRuleIp.parentRuleId)!!

            // 踢除ip访问规则缓存
            CacheKit.evict(CACHE_NAME, getKey(sysAccessRule.systemCode, sysAccessRule.tenantId))

            // 重新缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(
                    sysAccessRule.systemCode, sysAccessRule.tenantId
                )
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param ipRuleId ip访问规则id
     */
    open fun syncOnDelete(ipRuleId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${ipRuleId}的ip访问规则后，同步从${CACHE_NAME}缓存中踢除...")
            val sysAccessRuleIp = sysAccessRuleIpDao.get(ipRuleId)
            if (sysAccessRuleIp == null) {
                log.error("数据库中找不到id为${ipRuleId}的ip访问规则！")
                return
            }

            val sysAccessRule = sysAccessRuleDao.get(sysAccessRuleIp.parentRuleId)!!

            // 踢除缓存
            CacheKit.evict(CACHE_NAME, getKey(sysAccessRule.systemCode, sysAccessRule.tenantId))
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                // 重新缓存
                getSelf<AccessRuleIpsBySubSysAndTenantIdCache>().getAccessRuleIps(
                    sysAccessRule.systemCode, sysAccessRule.tenantId
                )
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun mapToCacheItems(ruleIpRecords: List<SysAccessRuleIpRecord>): List<SysAccessRuleIpCacheItem> {
        return ruleIpRecords.map { it ->
            SysAccessRuleIpCacheItem().apply {
                id = it.id
                ipStart = it.ipStart
                ipEnd = it.ipEnd
                ipTypeDictCode = it.ipTypeDictCode
                expirationTime = it.expirationTime
            }
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param systemCode 系统编码
     * @param tenantId 租户id
     * @return 缓存key
     */
    open fun getKey(systemCode: String, tenantId: String? = null): String {
        return "${systemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${tenantId}"
    }

    private val log = LogFactory.getLog(this)

}
