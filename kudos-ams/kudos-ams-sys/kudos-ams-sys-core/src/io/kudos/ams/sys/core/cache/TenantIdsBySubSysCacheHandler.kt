package io.kudos.ams.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.core.dao.SysTenantSubSystemDao
import io.kudos.ams.sys.core.model.po.SysTenantSubSystem
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 租户（by sub system code）缓存处理器
 *
 * 1.数据来源表：sys_tenant_sub_system
 * 2.缓存各子系统下的租户id
 * 3.缓存的key为：subSystemCode
 * 4.缓存的value为：租户id
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class TenantIdsBySubSysCacheHandler : AbstractCacheHandler<List<String>>() {

    @Autowired
    private lateinit var sysTenantSubSystemDao: SysTenantSubSystemDao

    companion object Companion {
        private const val CACHE_NAME = "SYS_TENANT_IDS_BY_SUB_SYS"
    }


    override fun cacheName(): String = CACHE_NAME

    override fun doReload(key: String): List<String> = getSelf<TenantIdsBySubSysCacheHandler>().getTenantIds(key)


    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有子系统下的租户id！")
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
            CacheKit.put(CACHE_NAME, it.key, it.value)
            log.debug("缓存了子系统${it.key}的${it.value}条租户id。")
        }
    }

    /**
     * 根据子系统编码从缓存中获取其下所有租户id，如果缓存中不存在，则从数据库中加载，并回写缓存
     *
     * @param subSystemCode 子系统编码
     * @return List<租户id>
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#subSystemCode",
        unless = "#result == null || #result.isEmpty()"
    )
    open fun getTenantIds(subSystemCode: String): List<String> {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("缓存中不存在子系统为${subSystemCode}的租户id，从数据库中加载...")
        }

        val tenantIds = sysTenantSubSystemDao.oneSearchProperty(
            SysTenantSubSystem::subSystemCode.name, subSystemCode, SysTenantSubSystem::tenantId.name
        )
        log.debug("从数据库加载了子系统为${subSystemCode}的${tenantIds.size}条租户id。")
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
            log.debug("新增id为${id}的租户-子系统关系后，同步${CACHE_NAME}缓存...")
            val subSystemCode = BeanKit.getProperty(any, SysTenantSubSystem::subSystemCode.name) as String
            evict(subSystemCode) // 踢除缓存，因为缓存的粒度为子系统
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                getSelf<TenantIdsBySubSysCacheHandler>().getTenantIds(subSystemCode) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param tenantId 租户id
     * @param subSystemCodes 子系统编码集合
     */
    open fun syncOnDelete(tenantId: String, subSystemCodes: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除租户id为${tenantId}的租户-子系统关系后，同步从${CACHE_NAME}缓存中踢除...")
            subSystemCodes.forEach { subSystemCode ->
                evict(subSystemCode) // 踢除缓存，缓存的粒度为子系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantIdsBySubSysCacheHandler>().getTenantIds(subSystemCode) // 重新缓存
                }
                log.debug("${CACHE_NAME}缓存同步完成。")
            }
        }
    }

    /**
     * 批量删除数据库记录后同步缓存
     *
     * @param tenantIds 租户id集合
     * @param subSystemCodes 子系统编码集合
     */
    open fun syncOnBatchDelete(tenantIds: Collection<String>, subSystemCodes: Collection<String>) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("批量删除id为${tenantIds}的租户后，同步从${CACHE_NAME}缓存中踢除...")
            subSystemCodes.forEach { subSystemCode ->
                CacheKit.evict(CACHE_NAME, subSystemCode) // 踢除缓存，缓存的粒度为子系统
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<TenantIdsBySubSysCacheHandler>().getTenantIds(subSystemCode) // 重新缓存
                }
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private val log = LogFactory.getLog(this)

}