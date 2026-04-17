package io.kudos.ms.sys.core.domain.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.common.domain.vo.request.SysDomainQuery
import io.kudos.ms.sys.common.domain.vo.response.SysDomainDetail
import io.kudos.ms.sys.common.domain.vo.response.SysDomainRow
import io.kudos.ms.sys.core.domain.cache.DomainByNameCache
import io.kudos.ms.sys.core.tenant.cache.TenantByIdCache
import io.kudos.ms.sys.core.domain.dao.SysDomainDao
import io.kudos.ms.sys.core.domain.model.po.SysDomain
import io.kudos.ms.sys.core.domain.service.iservice.ISysDomainService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 域名业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class SysDomainService(
    dao: SysDomainDao,
    private val domainByNameCache: DomainByNameCache,
    private val tenantByIdCache: TenantByIdCache,
) : BaseCrudService<String, SysDomain, SysDomainDao>(dao), ISysDomainService {

    private val log = LogFactory.getLog(this::class)

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        val result = if (returnType == SysDomainCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            dao.get(id, SysDomainCacheEntry::class) as R?
        } else {
            super.get(id, returnType)
        }
        if (result is SysDomainDetail) {
            result.tenantName = tenantByIdCache.getTenantById(result.tenantId)?.name.orEmpty()
        }
        return result
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)
        val rows = result.data
        if (rows.isNotEmpty() && rows.first() is SysDomainRow) {
            val tenantIds = rows.map { (it as SysDomainRow).tenantId }
            val tenants = tenantByIdCache.getTenantsByIds(tenantIds)
            val idAndNameMap = tenants.mapValues { entry -> entry.value.name }
            rows.forEach { row ->
                val r = row as SysDomainRow
                r.tenantName = requireNotNull(idAndNameMap[r.tenantId]) { "tenantId=${r.tenantId} 未在缓存中" }
            }
        }
        return result
    }

    override fun getDomainFromCache(domainName: String): SysDomainCacheEntry? = domainByNameCache.getDomain(domainName)

    override fun getDomainsByTenantId(tenantId: String): List<SysDomainRow> =
        dao.search(SysDomainQuery(tenantId = tenantId), SysDomainRow::class)

    override fun getDomainsBySystemCode(systemCode: String): List<SysDomainRow> =
        dao.searchAs(Criteria(SysDomain::systemCode eq systemCode))

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val domain = SysDomain {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(domain),
            log = log,
            successMessage = "更新id为${id}的域名的启用状态为${active}。",
            failureMessage = "更新id为${id}的域名的启用状态为${active}失败！",
        ) {
            domainByNameCache.syncOnUpdate(null, id)
        }
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的域名。") {
            domainByNameCache.syncOnInsert(any, id)
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val id = requireDomainId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的域名。",
            failureMessage = "更新id为${id}的域名失败！",
        ) {
            domainByNameCache.syncOnUpdate(any, id)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val domain = dao.get(id)
        if (domain == null) {
            log.warn("删除id为${id}的域名时，发现其已不存在！")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的域名。",
            failureMessage = "删除id为${id}的域名失败！",
        ) {
            domainByNameCache.syncOnDelete(domain, id)
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        @Suppress("UNCHECKED_CAST")
        val domains = dao.inSearchById(ids)
        val domainNames = domains.map { it.domain }.toSet()
        val count = super.batchDelete(ids)
        log.debug("批量删除域名，期望删除${ids.size}条，实际删除${count}条。")
        domainByNameCache.syncOnBatchDelete(ids, domainNames)
        return count
    }

    private fun requireDomainId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新域名时不支持的入参类型: ${any::class.qualifiedName}")
}
