package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.tenant.SysTenantCacheEntry
import io.kudos.ms.sys.common.vo.tenant.request.SysTenantFormCreate
import io.kudos.ms.sys.common.vo.tenant.request.SysTenantFormUpdate
import io.kudos.ms.sys.common.vo.tenant.response.SysTenantDetail
import io.kudos.ms.sys.common.vo.tenant.response.SysTenantRow
import io.kudos.ms.sys.core.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.cache.TenantByIdCache
import io.kudos.ms.sys.core.dao.SysTenantDao
import io.kudos.ms.sys.core.model.po.SysTenant
import io.kudos.ms.sys.core.model.po.SysTenantSystem
import io.kudos.ms.sys.core.service.iservice.ISysTenantService
import io.kudos.ms.sys.core.service.iservice.ISysTenantSystemService
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * 租户业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
@Transactional
open class SysTenantService(
    dao: SysTenantDao
) : BaseCrudService<String, SysTenant, SysTenantDao>(dao), ISysTenantService {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var tenantByIdCache: TenantByIdCache

    @Resource
    private lateinit var sysTenantSystemHashCache: SysTenantSystemHashCache

    @Resource
    private lateinit var sysTenantSystemService: ISysTenantSystemService

    override fun <R : Any> get(id: String, returnType: KClass<R>): R? {
        return if (returnType == SysTenantCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            tenantByIdCache.getTenantById(id) as R?
        } else {
            val result = super.get(id, returnType)
            if (result is SysTenantDetail) {
                result.subSystemCodes = getSubSystemCodesString(id)
            }
            result
        }
    }

    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)
        result.data.forEach {
            if (it is SysTenantRow) {
                it.subSystemCodes = getSubSystemCodesString(it.id)
            }
        }
        return result
    }

    override fun getTenantFromCache(id: String): SysTenantCacheEntry? {
        return tenantByIdCache.getTenantById(id)
    }

    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> {
        if (ids.isEmpty()) return emptyMap()
        return tenantByIdCache.getTenantsByIds(ids)
    }

    override fun getTenantsForSubSystemFromCache(subSystemCode: String): List<SysTenantCacheEntry> {
        val tenantIds = sysTenantSystemHashCache.getTenantIdsBySubSystemCode(subSystemCode)
        return tenantByIdCache.getTenantsByIds(tenantIds).values.toList()
    }

    override fun getAllTenantsFromCache(): List<SysTenantCacheEntry> {
        return dao.searchAs<SysTenantCacheEntry>()
    }

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        log.debug("新增id为${id}的租户。")

        if (any is SysTenantFormCreate) {
            insertSysTenantSystems(id, any.subSystemCodes)
        }

        tenantByIdCache.syncOnInsert(any, id) // 同步缓存
        sysTenantSystemHashCache.syncOnInsert(any, id)
        return id
    }

    private fun insertSysTenantSystems(tenantId: String, subSystemCodes: Set<String>) {
        val tenantSystems = subSystemCodes.mapTo(mutableSetOf()) { subSystemCode ->
            SysTenantSystem().apply {
                this.systemCode = subSystemCode
                this.tenantId = tenantId
            }
        }
        sysTenantSystemService.batchInsert(tenantSystems)
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val success = super.update(any)
        val id = BeanKit.getProperty(any, SysTenant::id.name) as String
        if (success) {
            if (any is SysTenantFormUpdate) {
                val tenantId = any.id!!
                val subSystemCodes = sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)
                if (subSystemCodes != any.subSystemCodes) {
                    sysTenantSystemService.deleteByTenantId(tenantId)
                    insertSysTenantSystems(tenantId, any.subSystemCodes)
                }
            }

            tenantByIdCache.syncOnUpdate(any, id)
        } else {
            log.error("更新id为${id}的租户失败！")
        }
        return success
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val tenant = SysTenant {
            this.id = id
            this.active = active
        }
        val success = dao.update(tenant)
        if (success) {
            log.debug("更新id为${id}的租户的启用状态为${active}。")
            tenantByIdCache.syncOnUpdate(tenant, id)
        } else {
            log.error("更新id为${id}的租户的启用状态为${active}失败！")
        }
        return success
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的租户时，发现其已不存在！")
            return false
        }

        val count = sysTenantSystemService.deleteByTenantId(id)
        if (count > 0) {
            sysTenantSystemHashCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的租户的所有系统关系失败！")
            return false
        }

        val success = super.deleteById(id)
        if (success) {
            log.debug("删除id为${id}的租户成功！")
            tenantByIdCache.syncOnDelete(id)
        } else {
            log.error("删除id为${id}的租户失败！")
        }
        return success
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        var count = sysTenantSystemService.batchDeleteByTenantIds(ids)
        if (count >= 0) {
            count = super.batchDelete(ids)
            log.debug("批量删除租户，期望删除${ids.size}条，实际删除${count}条。")
            tenantByIdCache.syncOnBatchDelete(ids)
        }
        return count
    }

    override fun getTenantRecord(id: String): SysTenantRow? {
        val tenant = dao.get(id) ?: return null
        return toSysTenantRow(tenant)
    }

    override fun getTenantByName(name: String): SysTenantRow? {
        val criteria = Criteria(SysTenant::name eq name)
        val tenant = dao.search(criteria).firstOrNull() ?: return null
        return toSysTenantRow(tenant)
    }

    private fun toSysTenantRow(tenant: SysTenant): SysTenantRow {
        return SysTenantRow(
            id = tenant.id,
            name = tenant.name,
            timezone = tenant.timezone,
            defaultLanguageCode = tenant.defaultLanguageCode,
            createTime = tenant.createTime,
            remark = tenant.remark,
            active = tenant.active,
            builtIn = tenant.builtIn,
        )
    }

    override fun getSubSystemCodesFromCache(tenantId: String): Set<String> {
        return sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)
    }

    private fun getSubSystemCodesString(tenantId: String): String {
        return sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId).joinToString(", ")
    }


}
