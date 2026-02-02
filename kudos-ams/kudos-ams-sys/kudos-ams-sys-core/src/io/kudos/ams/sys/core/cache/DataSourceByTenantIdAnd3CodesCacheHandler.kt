package io.kudos.ams.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceSearchPayload
import io.kudos.ams.sys.core.dao.SysDataSourceDao
import io.kudos.ams.sys.core.model.po.SysDataSource
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 子系统租户数据源缓存处理器
 *
 * 1.数据来源表：sys_data_source
 * 2.仅缓存tenantId不为null且active=true的数据源
 * 3.缓存key为：租户id::子系统编码::微服务编码，其中微服务编码可能为null
 * 4.缓存value为：SysDataSourceCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DataSourceByTenantIdAnd3CodesCacheHandler : AbstractCacheHandler<SysDataSourceCacheItem>() {

    @Autowired
    private lateinit var sysDataSourceDao: SysDataSourceDao

    @Autowired
    private lateinit var dataSourceByIdCacheHandler: DataSourceByIdCacheHandler

    companion object Companion {
        private const val CACHE_NAME = "SYS_DATA_SOURCE_BY_TENANT_ID_AND_3_CODES"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysDataSourceCacheItem? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式非法!"
        }
        val parts = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return getSelf<DataSourceByTenantIdAnd3CodesCacheHandler>().getDataSource(
            parts[0], parts[1], parts[2]
        )
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的租户数据源！")
            return
        }

        // 加载所有tenantId不为null且active=true的数据源
        val payload = SysDataSourceSearchPayload().apply {
            returnEntityClass = SysDataSourceCacheItem::class
            active = true
            operators = mapOf(SysDataSourceCacheItem::tenantId.name to OperatorEnum.IS_NOT_NULL)
        }

        @Suppress("UNCHECKED_CAST")
        val results = sysDataSourceDao.search(payload) as List<SysDataSourceCacheItem>

        // 清除缓存
        if (clear) {
            clear()
        }

        // 缓存数据
        results.forEach {
            CacheKit.put(
                CACHE_NAME,
                getKey(it.tenantId!!, it.subSystemCode!!, it.microServiceCode),
                it
            )
        }
        log.debug("缓存了数据源共${results.size}条。")
    }

    /**
     * 从缓存中获取数据源，如果缓存中没有，从数据库中加载，并写入缓存.
     *
     * 缓存的key形式为：租户id::子系统编码::微服务编码，其中微服务编码可能为null
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @return SysDataSourceCacheItem，不存在时返回null
     */
    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#tenantId.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}')" +
                ".concat(#subSystemCode).concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}')" +
                ".concat(#microServiceCode ?: 'null')",
        unless = "#result == null"
    )
    open fun getDataSource(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String? = null
    ): SysDataSourceCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug(
                "${CACHE_NAME}缓存中不存在租户id为${tenantId}且子系统编码为${subSystemCode}" +
                        "且微服务编码为${microServiceCode}的数据源，从数据库中加载..."
            )
        }
        require(subSystemCode.isNotBlank()) { "获取数据源时，子系统代码必须指定！" }
        val searchPayload = SysDataSourceSearchPayload().apply {
            returnEntityClass = SysDataSourceCacheItem::class
            active = true
            this.subSystemCode = subSystemCode
            this.microServiceCode = microServiceCode
            this.tenantId = tenantId
            nullProperties = listOf(this::microServiceCode.name)
        }

        @Suppress("UNCHECKED_CAST")
        val results = sysDataSourceDao.search(searchPayload) as List<SysDataSourceCacheItem>
        return if (results.isEmpty()) {
            log.warn(
                "数据库中找不到租户id为${tenantId}且子系统编码为${subSystemCode}" +
                        "且微服务编码为${microServiceCode}的数据源！"
            )
            null
        } else {
            results.first()
        }
    }

    /**
     * 数据库插入记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 数据源id
     */
    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的数据源后，同步${CACHE_NAME}缓存...")
            val props = BeanKit.extract(any)
            val tenantId = props[SysDataSource::tenantId.name] as String?
            if (!tenantId.isNullOrBlank()) {
                val active = props[SysDataSource::active.name] as Boolean?
                if (active == null || active) {
                    val subSystemCode = props[SysDataSource::subSystemCode.name] as String
                    val microServiceCode = props[SysDataSource::microServiceCode.name] as String?
                    // 缓存
                    getSelf<DataSourceByTenantIdAnd3CodesCacheHandler>().getDataSource(
                        tenantId, subSystemCode, microServiceCode
                    )
                    log.debug("${CACHE_NAME}缓存同步完成。")
                } else {
                    log.debug("新增的数据源的active为false，不需要同步${CACHE_NAME}缓存。")
                }
            } else {
                log.debug("新增的数据源的tenantId为空，不需要同步${CACHE_NAME}缓存。")
            }
        }
    }

    /**
     * 更新数据库记录后同步缓存
     *
     * @param any 包含必要属性的对象
     * @param id 数据源id
     */
    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的数据源后，同步${CACHE_NAME}缓存...")
            val props = BeanKit.extract(any)
            val tenantId = props[SysDataSource::tenantId.name] as String?
            if (!tenantId.isNullOrBlank()) {
                val subSystemCode = props[SysDataSource::subSystemCode.name] as String
                val microServiceCode = props[SysDataSource::microServiceCode.name] as String?

                // 踢除数据源缓存
                CacheKit.evict(
                    CACHE_NAME,
                    getKey(tenantId, subSystemCode, microServiceCode)
                )

                // 重新缓存
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    val active = props[SysDataSource::active.name] as Boolean?
                    if (active == null || active) {
                        getSelf<DataSourceByTenantIdAnd3CodesCacheHandler>().getDataSource(
                            tenantId, subSystemCode, microServiceCode
                        )
                    }
                }
                log.debug("${CACHE_NAME}缓存同步完成。")
            } else {
                log.debug("更新的数据源的tenantId为空，不需要同步${CACHE_NAME}缓存。")
            }
        }
    }

    /**
     * 更新启用状态后同步缓存
     *
     * @param id 数据源id
     * @param active 是否启用
     */
    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的数据源的启用状态后，同步${CACHE_NAME}缓存...")
            val ds = dataSourceByIdCacheHandler.getDataSourceById(id)
            if (ds == null) {
                log.error("缓存${dataSourceByIdCacheHandler.cacheName()}中找不到id为${id}的数据源！")
                return
            }

            val tenantId = ds.tenantId
            if (!tenantId.isNullOrBlank()) {
                // 踢除数据源缓存
                CacheKit.evict(
                    CACHE_NAME,
                    getKey(tenantId, ds.subSystemCode!!, ds.microServiceCode)
                )

                // 重新缓存
                if (active && CacheKit.isWriteInTime(CACHE_NAME)) {
                    getSelf<DataSourceByTenantIdAnd3CodesCacheHandler>().getDataSource(
                        tenantId, ds.subSystemCode!!, ds.microServiceCode
                    )
                }
                log.debug("${CACHE_NAME}缓存同步完成。")
            } else {
                log.debug("更新的数据源的tenantId为空，不需要同步${CACHE_NAME}缓存。")
            }
        }
    }

    /**
     * 删除数据库记录后同步缓存
     *
     * @param id 数据源id
     */
    open fun syncOnDelete(id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的数据源后，同步从${CACHE_NAME}缓存中踢除...")
            val ds = dataSourceByIdCacheHandler.getDataSourceById(id)
            if (ds == null) {
                log.error("缓存${dataSourceByIdCacheHandler.cacheName()}中找不到id为${id}的对象！")
                return
            }

            val tenantId = ds.tenantId
            if (!tenantId.isNullOrBlank()) {
                // 踢除缓存
                CacheKit.evict(
                    CACHE_NAME,
                    getKey(tenantId, ds.subSystemCode!!, ds.microServiceCode)
                )
                log.debug("${CACHE_NAME}缓存同步完成。")
            } else {
                log.debug("删除的数据源的tenantId为空，不需要同步${CACHE_NAME}缓存。")
            }
        }
    }

    /**
     * 返回参数拼接后的key
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @return 缓存key
     */
    open fun getKey(
        tenantId: String?,
        subSystemCode: String?,
        microServiceCode: String?
    ): String {
        return "${tenantId}${Consts.CACHE_KEY_DEFAULT_DELIMITER}" +
                "${subSystemCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}" +
                "$microServiceCode"
    }

    private val log = LogFactory.getLog(this)

}
