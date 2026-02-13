package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.security.CryptoKit
import io.kudos.base.support.payload.ListSearchPayload
import io.kudos.ms.sys.common.api.ISysTenantApi
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceDetail
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceRecord
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceSearchPayload
import io.kudos.ms.sys.common.vo.datasource.TenantIdAndASCodePayload
import io.kudos.ms.sys.core.cache.SysDataSourceHashCache
import io.kudos.ms.sys.core.dao.SysDataSourceDao
import io.kudos.ms.sys.core.model.po.SysDataSource
import io.kudos.ms.sys.core.service.iservice.ISysDataSourceService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 数据源业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDataSourceService : BaseCrudService<String, SysDataSource, SysDataSourceDao>(), ISysDataSourceService {
//endregion your codes 1

    //region your codes 2

    @Resource
    private lateinit var sysTenantApi: ISysTenantApi

    @Resource
    private lateinit var sysDataSourceHashCache: SysDataSourceHashCache

    private val log = LogFactory.getLog(this)

    override fun getDataSource(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String?
    ): List<SysDataSourceCacheItem> {
        return sysDataSourceHashCache.getDataSources(tenantId, subSystemCode, microServiceCode)
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): Pair<List<*>, Int> {
        val pair = super.pagingSearch(listSearchPayload)

        // 根据租户id获取租户名称
        val records = pair.first.filterIsInstance<SysDataSourceRecord>()
        if (records.isNotEmpty()) {
            val tenantIds = records.mapNotNull { it.tenantId }
            val tenants = sysTenantApi.getTenants(tenantIds)
            records.forEach {
                it.tenantName = tenants[it.tenantId]?.name
            }
        }

        return pair
    }

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        val result = super.get(id, returnType)
        if (returnType == SysDataSourceDetail::class) {
            val tenantId = (result as SysDataSourceDetail).tenantId
            result.tenantName = if (tenantId.isNullOrBlank()) null else sysTenantApi.getTenant(tenantId)?.name
        }
        return result
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的数据源。")
        // 同步缓存
        sysDataSourceHashCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysDataSource::id.name) as String
        if (success) {
            log.debug("更新id为${id}的数据源。")
            // 同步缓存
            sysDataSourceHashCache.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的数据源失败！")
        }
        return success
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dataSource = SysDataSource {
            this.id = id
            this.active = active
        }
        val success = dao.update(dataSource)
        if (success) {
            // 同步缓存
            sysDataSourceHashCache.syncOnUpdateActive(id, active)
        } else {
            log.error("更新id为${id}的数据源的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun resetPassword(id: String, newPassword: String) {
        val newPwd = CryptoKit.aesEncrypt(newPassword) // 加密密码
        val dataSource = SysDataSource {
            this.id = id
            this.password = newPwd
        }
        val success = dao.update(dataSource)
        if (success) {
            // 同步缓存
            val ds = requireNotNull(get(id)) { "重置数据源密码后找不到id=${id}的数据源。" }
            sysDataSourceHashCache.syncOnUpdate(ds, id)
        } else {
            log.error("重置id为${id}的数据源密码失败！")
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val success = super.deleteById(id)
        if (success) {
            // 同步缓存
            sysDataSourceHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的数据源失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除数据源，期望删除${ids.size}条，实际删除${count}条。")
        // 同步缓存
        sysDataSourceHashCache.syncOnBatchDelete(ids)
        return count
    }

    /**
     * 获取租户的数据源列表
     *
     * @param tenantId 租户id
     * @return 数据源记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getDataSourcesByTenantId(tenantId: String): List<SysDataSourceRecord> {
        val searchPayload = SysDataSourceSearchPayload().apply {
            this.tenantId = tenantId
        }
        return dao.search(searchPayload, SysDataSourceRecord::class)
    }

    /**
     * 获取子系统的数据源列表
     *
     * @param subSystemCode 子系统编码
     * @return 数据源记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getDataSourcesBySubSystemCode(subSystemCode: String): List<SysDataSourceRecord> {
        val searchPayload = SysDataSourceSearchPayload().apply {
            this.subSystemCode = subSystemCode
        }
        return dao.search(searchPayload, SysDataSourceRecord::class)
    }

    override fun getDataSource(payload: TenantIdAndASCodePayload): SysDataSourceCacheItem? {
        TODO("Not yet implemented")
    }

    //endregion your codes 2

}
