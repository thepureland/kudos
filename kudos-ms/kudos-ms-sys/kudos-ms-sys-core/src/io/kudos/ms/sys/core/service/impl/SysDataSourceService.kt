package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.security.CryptoKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.api.ISysTenantApi
import io.kudos.ms.sys.common.vo.datasource.SysDataSourceCacheEntry
import io.kudos.ms.sys.common.vo.datasource.response.SysDataSourceDetail
import io.kudos.ms.sys.common.vo.datasource.response.SysDataSourceRow
import io.kudos.ms.sys.core.cache.SysDataSourceHashCache
import io.kudos.ms.sys.core.dao.SysDataSourceDao
import io.kudos.ms.sys.core.model.po.SysDataSource
import io.kudos.ms.sys.core.service.iservice.ISysDataSourceService
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
@Transactional
open class SysDataSourceService(
    dao: SysDataSourceDao,
    private val sysTenantApi: ISysTenantApi,
    private val sysDataSourceHashCache: SysDataSourceHashCache,
) : BaseCrudService<String, SysDataSource, SysDataSourceDao>(dao), ISysDataSourceService {

    private val log = LogFactory.getLog(this::class)

    override fun getDataSourcesFromCache(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String?
    ): List<SysDataSourceCacheEntry> = sysDataSourceHashCache.getDataSources(tenantId, subSystemCode, microServiceCode)

    override fun getDataSourceFromCache(tenantId: String, atomicServiceCode: String?): SysDataSourceCacheEntry? =
        sysDataSourceHashCache.getDataSources(tenantId, null, atomicServiceCode).firstOrNull()

    override fun getDataSourceFromCache(id: String): SysDataSourceCacheEntry? = sysDataSourceHashCache.getDataSourceById(id)

    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)

        // 根据租户id获取租户名称
        val records = result.data.filterIsInstance<SysDataSourceRow>()
        if (records.isNotEmpty()) {
            val tenantIds = records.mapNotNull { it.tenantId }
            val tenants = sysTenantApi.getTenantsFromCacheByIds(tenantIds)
            records.forEach {
                it.tenantName = tenants[it.tenantId]?.name
            }
        }

        return result
    }

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysDataSourceCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysDataSourceHashCache.getDataSourceById(id) as R?
        } else {
            val result = super.get(id, returnType)
            if (returnType == SysDataSourceDetail::class && result != null) {
                val detail = result as SysDataSourceDetail
                val tenantId = detail.tenantId
                detail.tenantName = if (tenantId.isNullOrBlank()) null else sysTenantApi.getTenantFromCache(tenantId)?.name
            }
            result
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的数据源。") {
            sysDataSourceHashCache.syncOnInsert(any, id)
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireDataSourceId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的数据源。",
            failureMessage = "更新id为${id}的数据源失败！",
        ) {
            sysDataSourceHashCache.syncOnUpdate(any, id)
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dataSource = SysDataSource {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(dataSource),
            log = log,
            successMessage = "更新id为${id}的数据源的启用状态为${active}。",
            failureMessage = "更新id为${id}的数据源的启用状态为${active}失败！",
        ) {
            sysDataSourceHashCache.syncOnUpdate(dataSource, id)
        }
    }

    @Transactional
    override fun resetPassword(id: String, newPassword: String) {
        val newPwd = CryptoKit.aesEncrypt(newPassword) // 加密密码
        val dataSource = SysDataSource {
            this.id = id
            this.password = newPwd
        }
        completeCrudUpdate(
            success = dao.update(dataSource),
            log = log,
            successMessage = "重置id为${id}的数据源密码。",
            failureMessage = "重置id为${id}的数据源密码失败！",
        ) {
            val ds = requireNotNull(get(id)) { "重置数据源密码后找不到id=${id}的数据源。" }
            sysDataSourceHashCache.syncOnUpdate(ds, id)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val existing = dao.get(id)
        if (existing == null) {
            log.warn("删除id为${id}的数据源时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的数据源成功！",
            failureMessage = "删除id为${id}的数据源失败！",
        ) {
            sysDataSourceHashCache.syncOnDelete(id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除数据源，期望删除${ids.size}条，实际删除${count}条。")
        sysDataSourceHashCache.syncOnBatchDelete(ids)
        return count
    }

    override fun getDataSourcesByTenantId(tenantId: String): List<SysDataSourceRow> =
        dao.searchAs(Criteria(SysDataSource::tenantId eq tenantId))

    override fun getDataSourcesBySubSystemCode(subSystemCode: String): List<SysDataSourceRow> =
        dao.searchAs(Criteria(SysDataSource::subSystemCode eq subSystemCode))

    private fun requireDataSourceId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新数据源时不支持的入参类型: ${any::class.qualifiedName}")
}
