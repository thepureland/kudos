package io.kudos.ms.sys.core.datasource.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.security.CryptoKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.tenant.api.ISysTenantApi
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceDetail
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import io.kudos.ms.sys.core.datasource.cache.SysDataSourceHashCache
import io.kudos.ms.sys.core.datasource.dao.SysDataSourceDao
import io.kudos.ms.sys.core.datasource.event.SysDataSourceBatchDeleted
import io.kudos.ms.sys.core.datasource.event.SysDataSourceDeleted
import io.kudos.ms.sys.core.datasource.event.SysDataSourceInserted
import io.kudos.ms.sys.core.datasource.event.SysDataSourceUpdated
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import org.springframework.context.ApplicationEventPublisher
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
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysDataSource, SysDataSourceDao>(dao), ISysDataSourceService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun getDataSourcesFromCache(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String?
    ): List<SysDataSourceCacheEntry> = sysDataSourceHashCache.getDataSources(tenantId, subSystemCode, microServiceCode)

    @Transactional(readOnly = true)
    override fun getDataSourceFromCache(tenantId: String, atomicServiceCode: String?): SysDataSourceCacheEntry? =
        sysDataSourceHashCache.getDataSources(tenantId, null, atomicServiceCode).firstOrNull()

    @Transactional(readOnly = true)
    override fun getDataSourceFromCache(id: String): SysDataSourceCacheEntry? = sysDataSourceHashCache.getDataSourceById(id)

    @Transactional(readOnly = true)
    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)
        enrichTenantNames(result.data.filterIsInstance<SysDataSourceRow>())
        return result
    }

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysDataSourceCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            sysDataSourceHashCache.getDataSourceById(id) as R?
        } else {
            enrichDataSourceDetail(super.get(id, returnType), returnType)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的数据源。") {
            eventPublisher.publishEvent(SysDataSourceInserted(id = id))
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
            eventPublisher.publishEvent(SysDataSourceUpdated(id = id))
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
            eventPublisher.publishEvent(SysDataSourceUpdated(id = id))
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
            eventPublisher.publishEvent(SysDataSourceUpdated(id = id))
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
            eventPublisher.publishEvent(SysDataSourceDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("批量删除数据源，期望删除${ids.size}条，实际删除${count}条。")
        if (count > 0) {
            eventPublisher.publishEvent(SysDataSourceBatchDeleted(ids = ids))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getDataSourcesByTenantId(tenantId: String): List<SysDataSourceRow> =
        dao.searchAs(Criteria(SysDataSource::tenantId eq tenantId))

    @Transactional(readOnly = true)
    override fun getDataSourcesBySubSystemCode(subSystemCode: String): List<SysDataSourceRow> =
        dao.searchAs(Criteria(SysDataSource::subSystemCode eq subSystemCode))

    private fun enrichTenantNames(records: List<SysDataSourceRow>) {
        if (records.isEmpty()) return
        val tenants = sysTenantApi.getTenantsFromCacheByIds(records.mapNotNull { it.tenantId })
        records.forEach { record ->
            record.tenantName = tenants[record.tenantId]?.name
        }
    }

    private fun <R : Any> enrichDataSourceDetail(result: R?, returnType: KClass<R>): R? {
        if (returnType == SysDataSourceDetail::class && result is SysDataSourceDetail) {
            result.tenantName = result.tenantId
                ?.takeUnless(String::isBlank)
                ?.let { sysTenantApi.getTenantFromCache(it)?.name }
        }
        return result
    }

    private fun requireDataSourceId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新数据源时不支持的入参类型: ${any::class.qualifiedName}")
}
