package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ms.sys.core.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.model.po.SysTenantSystem
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 租户（by system code）缓存处理器
 *
 * 1.数据来源表：sys_tenant_system
 * 2.缓存各系统下的租户id
 * 3.缓存的key为：systemCode
 * 4.缓存的value为：租户id
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantIdsBySystemCodeCache : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var sysTenantSystemDao: SysTenantSystemDao

    companion object Companion {
        private const val CACHE_NAME = "SYS_TENANT_IDS_BY_SYSTEM_CODE"
    }


    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<TenantIdsBySystemCodeCache>().getTenantIds(key)


    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有系统下的租户id！")
            return
        }

        // 先加载所有租户和系统的关系
        val subSysCodeAndTenantIdsMap = sysTenantSystemDao.groupingTenantIdsBySystemCodes()
        log.debug("从数据库加载了${subSysCodeAndTenantIdsMap.values.flatten().size}条租户-系统关系信息。")

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存租户
        subSysCodeAndTenantIdsMap.forEach {
            CacheKit.put(CACHE_NAME, it.key, it.value)
            log.debug("缓存了系统${it.key}的${it.value}条租户id。")
        }
    }

    /**
     * 根据系统编码从缓存中获取其下所有租户id，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param systemCode 系统编码
     * @return List<租户id>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#systemCode",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getTenantIds(systemCode: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在系统为${systemCode}的租户id，从数据库中加载...")
        }

        val tenantIds = sysTenantSystemDao.oneSearchProperty(
            SysTenantSystem::systemCode.name, systemCode, SysTenantSystem::tenantId.name
        )
        log.debug("从数据库加载了系统为${systemCode}的${tenantIds.size}条租户id。")
        @Suppress("UNCHECKED_CAST")
        return tenantIds as List<String>
    }

//    @CacheEvict(cacheNames = [CACHE_NAME], key = "#subSystemCode", beforeInvocation = true)
//    override fun evict(subSystemCode: String) {
//    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 主键
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("新增id为${id}的租户-系统关系后，同步${CACHE_NAME}缓存...")
            val systemCode = BeanKit.getProperty(any, SysTenantSystem::systemCode.name) as String
            evict(systemCode) // 踢除缓存，因为缓存的粒度为系统
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<TenantIdsBySystemCodeCache>().getTenantIds(systemCode) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param tenantId 租户id
     * @param systemCodes 系统编码集合
     */
    open fun syncOnDelete(tenantId: String, systemCodes: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户id为${tenantId}的租户-系统关系后，同步从${CACHE_NAME}缓存中踢除...")
            systemCodes.forEach { systemCode ->
                evict(systemCode) // 踢除缓存，缓存的粒度为系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantIdsBySystemCodeCache>().getTenantIds(systemCode) // 重新缓存
                }
                log.debug("${CACHE_NAME}缓存同步完成。")
            }
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param tenantIds 租户id集合
     * @param systemCodes 系统编码集合
     */
    open fun syncOnBatchDelete(tenantIds: Collection<String>, systemCodes: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${tenantIds}的租户后，同步从${CACHE_NAME}缓存中踢除...")
            systemCodes.forEach { systemCode ->
                CacheKit.evict(CACHE_NAME, systemCode) // 踢除缓存，缓存的粒度为系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantIdsBySystemCodeCache>().getTenantIds(systemCode) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}
