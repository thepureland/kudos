package io.kudos.ms.sys.core.service.impl

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
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
    dao: SysTenantDao,
    private val tenantByIdCache: TenantByIdCache,
    private val sysTenantSystemHashCache: SysTenantSystemHashCache,
    private val sysTenantSystemService: ISysTenantSystemService,
) : BaseCrudService<String, SysTenant, SysTenantDao>(dao), ISysTenantService {

    private val log = LogFactory.getLog(this::class)

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

    override fun getTenantFromCache(id: String): SysTenantCacheEntry? = tenantByIdCache.getTenantById(id)

    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> =
        ids.takeIf { it.isNotEmpty() }?.let(tenantByIdCache::getTenantsByIds) ?: emptyMap()

    override fun getTenantsForSubSystemFromCache(subSystemCode: String): List<SysTenantCacheEntry> =
        tenantByIdCache.getTenantsByIds(sysTenantSystemHashCache.getTenantIdsBySubSystemCode(subSystemCode)).values.toList()

    override fun getAllTenantsFromCache(): List<SysTenantCacheEntry> = dao.searchAs()

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "新增id为${id}的租户。") {
            if (any is SysTenantFormCreate) {
                insertSysTenantSystems(id, any.subSystemCodes)
            }
            tenantByIdCache.syncOnInsert(any, id)
            sysTenantSystemHashCache.syncOnInsert(any, id)
        }
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
        val id = requireTenantId(any)
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "更新id为${id}的租户。",
            failureMessage = "更新id为${id}的租户失败！",
        ) {
            syncTenantSystemsOnUpdate(any)
            tenantByIdCache.syncOnUpdate(any, id)
        }
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val tenant = SysTenant {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(tenant),
            log = log,
            successMessage = "更新id为${id}的租户的启用状态为${active}。",
            failureMessage = "更新id为${id}的租户的启用状态为${active}失败！",
        ) {
            tenantByIdCache.syncOnUpdate(tenant, id)
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("删除id为${id}的租户时，发现其已不存在！")
            return false
        }

        sysTenantSystemService.deleteByTenantId(id)
        sysTenantSystemHashCache.syncOnDelete(id)

        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "删除id为${id}的租户成功！",
            failureMessage = "删除id为${id}的租户失败！",
        ) {
            tenantByIdCache.syncOnDelete(id)
        }
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

    override fun getTenantRecord(id: String): SysTenantRow? = dao.get(id)?.let(::toSysTenantRow)

    override fun getTenantByName(name: String): SysTenantRow? =
        dao.search(Criteria(SysTenant::name eq name)).firstOrNull()?.let(::toSysTenantRow)

    private fun syncTenantSystemsOnUpdate(any: Any) {
        if (any !is SysTenantFormUpdate) return

        val tenantId = requireNotNull(any.id) { "更新租户时 id 不能为空" }
        val subSystemCodes = sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)
        if (subSystemCodes != any.subSystemCodes) {
            sysTenantSystemService.deleteByTenantId(tenantId)
            insertSysTenantSystems(tenantId, any.subSystemCodes)
        }
    }

    private fun toSysTenantRow(tenant: SysTenant): SysTenantRow = SysTenantRow(
        id = tenant.id,
        name = tenant.name,
        timezone = tenant.timezone,
        defaultLanguageCode = tenant.defaultLanguageCode,
        createTime = tenant.createTime,
        remark = tenant.remark,
        active = tenant.active,
        builtIn = tenant.builtIn,
    )

    override fun getSubSystemCodesFromCache(tenantId: String): Set<String> =
        sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)

    private fun getSubSystemCodesString(tenantId: String): String =
        sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId).joinToString(", ")

    private fun requireTenantId(any: Any): String =
        (any as? IIdEntity<*>)?.id as? String
            ?: error("更新租户时不支持的入参类型: ${any::class.qualifiedName}")
}
