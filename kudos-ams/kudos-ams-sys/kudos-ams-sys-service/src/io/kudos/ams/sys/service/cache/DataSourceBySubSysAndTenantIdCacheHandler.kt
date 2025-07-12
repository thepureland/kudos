package io.kudos.ams.sys.service.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.cache.common.support.AbstractCacheHandler
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ams.sys.common.vo.datasource.SysDataSourceSearchPayload
import io.kudos.ams.sys.service.dao.SysDataSourceDao
import io.kudos.ams.sys.service.model.po.SysDataSource
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.Consts
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component


/**
 * 子系统租户数据源缓存处理器
 *
 * 1. 仅缓存租户id非空且active=true的数据源
 * 2. 缓存key为：子系统编码::租户id
 * 3. 缓存value为：SysDataSourceCacheItem对象
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class DataSourceBySubSysAndTenantIdCacheHandler : AbstractCacheHandler<SysDataSourceCacheItem>() {

    @Autowired
    private lateinit var sysDataSourceDao: SysDataSourceDao

    @Autowired
    private lateinit var self: DataSourceBySubSysAndTenantIdCacheHandler


    companion object {
        private const val CACHE_NAME = "sys_data_source_by_sub_sys_and_tenant_id"
    }

    override fun cacheName() = CACHE_NAME

    override fun doReload(key: String): SysDataSourceCacheItem? {
        require(key.contains(Consts.CACHE_KEY_DEFAULT_DELIMITER)) {
            "缓存${CACHE_NAME}的key格式必须是 子系统代码${Consts.CACHE_KEY_DEFAULT_DELIMITER}租户id"
        }
        val subSysAndTenantId = key.split(Consts.CACHE_KEY_DEFAULT_DELIMITER)
        return self.getDataSource(subSysAndTenantId[0], subSysAndTenantId[1])
    }

    override fun reloadAll(clear: Boolean) {
        if (!CacheKit.isCacheActive(CACHE_NAME)) {
            log.info("缓存未开启，不加载和缓存所有启用状态的租户数据源！")
            return
        }

        // 加载所有可用的数据源
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
            CacheKit.putIfAbsent(CACHE_NAME, getKey(it.subSystemCode!!, it.tenantId!!), it)
        }
        log.debug("缓存了数据源共${results.size}条。")
    }

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#subSystemCode.concat('${Consts.CACHE_KEY_DEFAULT_DELIMITER}').concat(#tenantId)",
        unless = "#result == null"
    )
    open fun getDataSource(subSystemCode: String, tenantId: String?): SysDataSourceCacheItem? {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("${CACHE_NAME}缓存中不存在子系统为${subSystemCode}且租户id为${tenantId}的数据源，从数据库中加载...")
        }
        require(subSystemCode.isNotBlank()) { "获取数据源时，子系统代码必须指定！" }
        val searchPayload = SysDataSourceSearchPayload().apply {
            returnEntityClass = SysDataSourceCacheItem::class
            active = true
            this.subSystemCode = subSystemCode
            this.tenantId = tenantId
        }

        @Suppress("UNCHECKED_CAST")
        val results = sysDataSourceDao.search(searchPayload) as List<SysDataSourceCacheItem>
        return if (results.isEmpty()) {
            log.warn("数据库中找不到子系统为${subSystemCode}且租户id为${tenantId}的数据源！")
            null
        } else {
            val result = results.first()
            if (results.size > 1) {
                log.warn("数据库中找到${results.size}条子系统为${subSystemCode}且租户id为${tenantId}的数据源，任取一条！")
            } else {
                log.debug("从数据库加载到子系统为${subSystemCode}且租户id为${tenantId}的数据源#${result.id}")
            }
            result
        }
    }

    open fun syncOnInsert(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME) && CacheKit.isWriteInTime(CACHE_NAME)) {
            log.debug("新增id为${id}的数据源后，同步${CACHE_NAME}缓存...")
            val subSysDictCode = BeanKit.getProperty(any, SysDataSource::subSystemCode.name) as String
            val tenantId = BeanKit.getProperty(any, SysDataSource::tenantId.name) as String?
            self.getDataSource(subSysDictCode, tenantId) // 缓存
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdate(any: Any, id: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的数据源后，同步${CACHE_NAME}缓存...")
            val subSysDictCode = BeanKit.getProperty(any, SysDataSource::subSystemCode.name) as String
            val tenantId = BeanKit.getProperty(any, SysDataSource::tenantId.name) as String
            CacheKit.evict(CACHE_NAME, getKey(subSysDictCode, tenantId)) // 踢除数据源缓存
            if (CacheKit.isWriteInTime(CACHE_NAME)) {
                self.getDataSource(subSysDictCode, tenantId) // 重新缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnUpdateActive(id: String, active: Boolean) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("更新id为${id}的数据源的启用状态后，同步${CACHE_NAME}缓存...")
            val dataSource = sysDataSourceDao.get(id)!!
            if (active) {
                if (CacheKit.isWriteInTime(CACHE_NAME)) {
                    self.getDataSource(dataSource.subSystemCode, dataSource.tenantId) // 重新缓存
                }
            } else {
                CacheKit.evict(CACHE_NAME, getKey(dataSource.subSystemCode, dataSource.tenantId!!)) // 踢除数据源缓存
            }
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    open fun syncOnDelete(id: String, subSysDictCode: String, tenantId: String) {
        if (CacheKit.isCacheActive(CACHE_NAME)) {
            log.debug("删除id为${id}的数据源后，同步从${CACHE_NAME}缓存中踢除...")
            val key = "${subSysDictCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${tenantId}"
            CacheKit.evict(CACHE_NAME, key) // 踢除缓存, 数据源缓存的粒度到数据源类型
            log.debug("${CACHE_NAME}缓存同步完成。")
        }
    }

    private fun getKey(subSysDictCode: String, tenantId: String): String {
        return "${subSysDictCode}${Consts.CACHE_KEY_DEFAULT_DELIMITER}${tenantId}"
    }

    private val log = LogFactory.getLog(this)


}