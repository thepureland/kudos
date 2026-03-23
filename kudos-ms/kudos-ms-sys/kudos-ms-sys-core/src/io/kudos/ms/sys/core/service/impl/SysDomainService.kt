package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheEntry
import io.kudos.ms.sys.common.vo.domain.request.SysDomainQuery
import io.kudos.ms.sys.common.vo.domain.response.SysDomainDetail
import io.kudos.ms.sys.common.vo.domain.response.SysDomainRow
import io.kudos.ms.sys.core.cache.DomainByNameCache
import io.kudos.ms.sys.core.cache.TenantByIdCache
import io.kudos.ms.sys.core.dao.SysDomainDao
import io.kudos.ms.sys.core.model.po.SysDomain
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
import jakarta.annotation.Resource
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
    dao: SysDomainDao
) : BaseCrudService<String, SysDomain, SysDomainDao>(dao), ISysDomainService {


    private val log = LogFactory.getLog(this)

    @Resource
    private lateinit var domainByNameCache: DomainByNameCache

    @Resource
    private lateinit var tenantByIdCache: TenantByIdCache

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        val result = super.get(id, returnType)
        if (result is SysDomainDetail) {
            result.tenantName = tenantByIdCache.getTenantById(result.tenantId)!!.name
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
            rows.forEach { (it as SysDomainRow).tenantName = idAndNameMap[it.tenantId]!! }
        }
        return result
    }

    override fun getDomainByName(domainName: String): SysDomainCacheEntry? {
        return domainByNameCache.getDomain(domainName)
    }

    override fun getDomainsByTenantId(tenantId: String): List<SysDomainRow> {
        val searchPayload = SysDomainQuery(tenantId = tenantId)
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload, SysDomainRow::class)
    }

    override fun getDomainsBySystemCode(systemCode: String): List<SysDomainRow> {
        val criteria = Criteria(SysDomain::systemCode eq systemCode)
        return dao.searchAs<SysDomainRow>(criteria)
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val domain = SysDomain {
            this.id = id
            this.active = active
        }
        val success = dao.update(domain)
        if (success) {
            log.debug("更新id为${id}的域名的启用状态为${active}。")
            domainByNameCache.syncOnUpdate(null, id)
        } else {
            log.error("更新id为${id}的域名的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的域名。")
        domainByNameCache.syncOnInsert(any, id)
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysDomain::id.name) as String
        if (success) {
            log.debug("更新id为${id}的域名。")
            domainByNameCache.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的域名失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val domain = dao.get(id)
        if (domain == null) {
            log.warn("删除id为${id}的域名时，发现其已不存在！")
            return false
        }
        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的域名。")
            domainByNameCache.syncOnDelete(domain, id)
        } else {
            log.error("删除id为${id}的域名失败！")
        }
        return success
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


}
