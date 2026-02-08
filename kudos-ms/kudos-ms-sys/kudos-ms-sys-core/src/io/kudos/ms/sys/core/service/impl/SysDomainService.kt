package io.kudos.ms.sys.core.service.impl

import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheItem
import io.kudos.ms.sys.common.vo.domain.SysDomainRecord
import io.kudos.ms.sys.common.vo.domain.SysDomainSearchPayload
import io.kudos.ms.sys.core.cache.DomainByNameCache
import io.kudos.ms.sys.core.dao.SysDomainDao
import io.kudos.ms.sys.core.model.po.SysDomain
import io.kudos.ms.sys.core.service.iservice.ISysDomainService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 域名业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDomainService : BaseCrudService<String, SysDomain, SysDomainDao>(), ISysDomainService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var domainByNameCache: DomainByNameCache

    override fun getDomainByName(domain: String): SysDomainCacheItem? {
        return domainByNameCache.getDomain(domain)
    }

    override fun getDomainsByTenantId(tenantId: String): List<SysDomainRecord> {
        val searchPayload = SysDomainSearchPayload().apply {
            this.tenantId = tenantId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysDomainRecord>
    }

    override fun getDomainsBySystemCode(systemCode: String): List<SysDomainRecord> {
        val searchPayload = SysDomainSearchPayload().apply {
            this.systemCode = systemCode
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysDomainRecord>
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
        val domain = dao.getAs(id)
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

    //endregion your codes 2

}
