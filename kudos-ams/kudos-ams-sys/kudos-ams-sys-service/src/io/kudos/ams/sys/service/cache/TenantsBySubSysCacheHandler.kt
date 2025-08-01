package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.common.vo.tenant.SysTenantCacheItem
import io.kudos.ams.sys.common.vo.tenant.SysTenantSearchPayload
import io.kudos.ams.sys.service.dao.SysTenantDao
import io.kudos.ams.sys.service.dao.SysTenantSubSystemDao
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 租户（by sub system code）缓存处理器
 *
 * 1.缓存各子系统下的租户
 * 2.缓存的key为：subSystemCode
 * 3.缓存的value为：SysTenantCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantsBySubSysCacheHandler : AbstractCacheHandler<List<SysTenantCacheItem>>() {

    @Autowired
    private lateinit var tenantByIdCacheHandler: TenantByIdCacheHandler

    @Autowired
    private lateinit var sysTenantDao: SysTenantDao

    @Autowired
    private lateinit var sysTenantSubSystemDao: SysTenantSubSystemDao

    companion object {
        private const val CACHE_NAME = "SYS_TENANTS_BY_SUB_SYS"
    }


    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<SysTenantCacheItem>
        = getSelf<TenantsBySubSysCacheHandler>().getTenantsFromCache(key)


    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的租户！")
            return
        }

        // 先加载所有租户和子系统的关系
        val subSysCodeAndTenantIdsMap = sysTenantSubSystemDao.groupingTenantIdsBySubSystemCodes()
        log.debug("从数据库加载了${subSysCodeAndTenantIdsMap.values.flatten().size}条租户-子系统关系信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存租户
        subSysCodeAndTenantIdsMap.forEach {
            val tenants = tenantByIdCacheHandler.getTenantsByIds(it.value)
            CacheKit.putIfAbsent(CACHE_NAME, it.key, tenants)
            log.debug("缓存了子系统${it.key}的${tenants.size}条租户信息。")
        }
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#subSystemCode",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getTenantsFromCache(subSystemCode: String): List<SysTenantCacheItem> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在子系统为${subSystemCode}的租户，从数据库中加载...")
        }
        val searchPayload = SysTenantSearchPayload().apply {
            returnEntityClass = SysTenantCacheItem::class
            active = true
            this.subSystemCode = subSystemCode
        }

        @Suppress("UNCHECKED_CAST")
        val tenants = sysTenantDao.search(searchPayload) as List<SysTenantCacheItem>
        log.debug("从数据库加载了子系统为${subSystemCode}的${tenants.size}条租户信息。")
        return tenants
    }

    open fun syncOnInsert(any: Any, tenantId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${tenantId}的租户后，同步${CACHE_NAME}缓存...")
            val subSystemCodes = sysTenantSubSystemDao.searchSubSystemCodesByTenantId(tenantId)
            subSystemCodes.forEach { subSystemCode ->
                CacheKit.evict(CACHE_NAME, subSystemCode) // 踢除缓存，因为缓存的粒度为子系统
                getSelf<TenantsBySubSysCacheHandler>().getTenantsFromCache(subSystemCode) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(any: Any?, tenantId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${tenantId}的租户后，同步${CACHE_NAME}缓存...")
            val subSystemCodes = sysTenantSubSystemDao.searchSubSystemCodesByTenantId(tenantId)
            subSystemCodes.forEach { subSystemCode ->
                CacheKit.evict(CACHE_NAME, subSystemCode) // 踢除缓存，因为缓存的粒度为子系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantsBySubSysCacheHandler>().getTenantsFromCache(subSystemCode) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnDelete(sysTenant: SysTenantCacheItem) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            val tenantId = sysTenant.id!!
            log.debug("删除id为${tenantId}的租户后，同步从${CACHE_NAME}缓存中踢除...")
            val subSystemCodes = sysTenantSubSystemDao.searchSubSystemCodesByTenantId(tenantId)
            subSystemCodes.forEach { subSystemCode ->
                CacheKit.evict(CACHE_NAME, subSystemCode) // 踢除缓存，缓存的粒度为子系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantsBySubSysCacheHandler>().getTenantsFromCache(subSystemCode) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnBatchDelete(tenantIds: Collection<String>, subSystemCodes: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${tenantIds}的租户后，同步从${CACHE_NAME}缓存中踢除...")
            subSystemCodes.forEach {
                CacheKit.evict(CACHE_NAME, it) // 踢除缓存，缓存的粒度为子系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantsBySubSysCacheHandler>().getTenantsFromCache(it) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}