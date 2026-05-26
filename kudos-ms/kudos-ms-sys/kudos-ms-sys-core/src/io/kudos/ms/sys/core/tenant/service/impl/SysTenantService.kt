package io.kudos.ms.sys.core.tenant.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import io.kudos.ms.sys.core.platform.service.impl.requireStringId

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.vo.IdAndName
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormCreate
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormUpdate
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantDetail
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.tenant.cache.TenantByIdCache
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.event.SysTenantBatchDeleted
import io.kudos.ms.sys.core.tenant.event.SysTenantDeleted
import io.kudos.ms.sys.core.tenant.event.SysTenantInserted
import io.kudos.ms.sys.core.tenant.event.SysTenantUpdated
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.reflect.KClass


/**
 * Tenant service.
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
    private val eventPublisher: ApplicationEventPublisher,
) : BaseCrudService<String, SysTenant, SysTenantDao>(dao), ISysTenantService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysTenantCacheEntry::class) {
            @Suppress("UNCHECKED_CAST")
            tenantByIdCache.getTenantById(id) as R?
        } else {
            enrichTenantDetail(super.get(id, returnType), id)
        }

    @Transactional(readOnly = true)
    override fun pagingSearch(listSearchPayload: ListSearchPayload): PagingSearchResult<*> {
        val result = super.pagingSearch(listSearchPayload)
        result.data.filterIsInstance<SysTenantRow>().forEach(::enrichTenantRow)
        return result
    }

    @Transactional(readOnly = true)
    override fun getTenantFromCache(id: String): SysTenantCacheEntry? = tenantByIdCache.getTenantById(id)

    @Transactional(readOnly = true)
    override fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry> =
        ids.takeIf { it.isNotEmpty() }?.let(tenantByIdCache::getTenantsByIds) ?: emptyMap()

    @Transactional(readOnly = true)
    override fun getTenantsForSubSystemFromCache(subSystemCode: String): List<SysTenantCacheEntry> =
        tenantByIdCache.getTenantsByIds(sysTenantSystemHashCache.getTenantIdsBySubSystemCode(subSystemCode)).values.toList()

    @Transactional(readOnly = true)
    override fun getActiveTenantIdAndNamesForSubSystem(subSystemCode: String): List<IdAndName<String>> =
        getTenantsForSubSystemFromCache(subSystemCode)
            .filter { it.active }
            .map { IdAndName(it.id, it.name) }

    @Transactional(readOnly = true)
    override fun getAllTenantsFromCache(): List<SysTenantCacheEntry> = dao.searchAs()

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted tenant id=$id.") {
            // insertTenantSystemsOnCreate creates the relations via sysTenantSystemService.batchInsert;
            // SysTenantSystemHashCache is lazily backfilled on demand, no explicit sync needed here.
            insertTenantSystemsOnCreate(any, id)
            eventPublisher.publishEvent(SysTenantInserted(id = id))
        }
        return id
    }

    /**
     * Batch insert rows into the "tenant ↔ sub-system" link table.
     *
     * Maps each code in `subSystemCodes` to a [SysTenantSystem] entity and writes them via `batchInsert`
     * in a single round trip to avoid N+1.
     *
     * @param tenantId tenant id
     * @param subSystemCodes sub-system code set
     * @author K
     * @since 1.0.0
     */
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
        val id = requireStringId(any, "tenant")
        return completeCrudUpdate(
            success = super.update(any),
            log = log,
            successMessage = "Updated tenant id=$id.",
            failureMessage = "Failed to update tenant id=$id!",
        ) {
            syncTenantSystemsOnUpdate(any)
            eventPublisher.publishEvent(SysTenantUpdated(id = id))
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
            successMessage = "Updated tenant id=$id active=$active.",
            failureMessage = "Failed to update tenant id=$id active=$active!",
        ) {
            eventPublisher.publishEvent(SysTenantUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        if (dao.get(id) == null) {
            log.warn("Tenant id=$id no longer exists when attempting delete!")
            return false
        }

        deleteTenantSystems(id)

        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted tenant id=$id.",
            failureMessage = "Failed to delete tenant id=$id!",
        ) {
            eventPublisher.publishEvent(SysTenantDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        if (sysTenantSystemService.batchDeleteByTenantIds(ids) < 0) return 0
        val count = super.batchDelete(ids)
        log.debug("Batch delete tenants: expected ${ids.size}, actually deleted $count.")
        if (count > 0) {
            eventPublisher.publishEvent(SysTenantBatchDeleted(ids = ids))
        }
        return count
    }

    @Transactional(readOnly = true)
    override fun getTenantRecord(id: String): SysTenantRow? = dao.get(id)?.let(::toSysTenantRow)

    @Transactional(readOnly = true)
    override fun getTenantByName(name: String): SysTenantRow? =
        dao.search(Criteria(SysTenant::name eq name)).firstOrNull()?.let(::toSysTenantRow)

    /**
     * On tenant update, sync the "tenant ↔ sub-system" bindings: only triggered for [SysTenantFormUpdate] payloads,
     * and only after diffing against the cache—skips redundant DB writes and avoids needless cache invalidation churn.
     *
     * @param any update payload; no-op when not a [SysTenantFormUpdate]
     * @author K
     * @since 1.0.0
     */
    private fun syncTenantSystemsOnUpdate(any: Any) {
        if (any !is SysTenantFormUpdate) return

        val tenantId = requireNotNull(any.id) { "tenant id must not be null on update" }
        val subSystemCodes = sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)
        if (subSystemCodes != any.subSystemCodes) {
            replaceTenantSystems(tenantId, any.subSystemCodes)
        }
    }

    /**
     * "Full replace" semantics: drop old links, then insert the new set.
     * Simple but avoids incremental diff complexity—binding count is small so performance is acceptable.
     *
     * @param tenantId tenant id
     * @param subSystemCodes new sub-system code set
     * @author K
     * @since 1.0.0
     */
    private fun replaceTenantSystems(tenantId: String, subSystemCodes: Set<String>) {
        deleteTenantSystems(tenantId)
        insertSysTenantSystems(tenantId, subSystemCodes)
    }

    /**
     * Clear all sub-system bindings for a tenant.
     * Cache invalidation is handled by the `SysTenantSystemTenantsChanged` event published inside
     * `sysTenantSystemService.deleteByTenantId`, which `SysTenantSystemHashCache` subscribes to.
     *
     * @param tenantId tenant id
     * @author K
     * @since 1.0.0
     */
    private fun deleteTenantSystems(tenantId: String) {
        // sysTenantSystemService.deleteByTenantId already publishes SysTenantSystemTenantsChanged,
        // which SysTenantSystemHashCache.on(...) subscribes to in order to invalidate.
        sysTenantSystemService.deleteByTenantId(tenantId)
    }

    /**
     * On tenant creation, conditionally insert sub-system bindings based on payload type: only triggers for [SysTenantFormCreate].
     *
     * @param any create payload
     * @param tenantId generated tenant id
     * @author K
     * @since 1.0.0
     */
    private fun insertTenantSystemsOnCreate(any: Any, tenantId: String) {
        if (any is SysTenantFormCreate) {
            insertSysTenantSystems(tenantId, any.subSystemCodes)
        }
    }

    /**
     * Detail enrichment: only when the result is a [SysTenantDetail] do we fill the `subSystemCodes` field; other return types pass through unchanged.
     *
     * @param R return type
     * @param result object to enrich
     * @param tenantId tenant id
     * @return enriched object (may be unchanged)
     * @author K
     * @since 1.0.0
     */
    private fun <R : Any> enrichTenantDetail(result: R?, tenantId: String): R? {
        if (result is SysTenantDetail) {
            result.subSystemCodes = getSubSystemCodesString(tenantId)
        }
        return result
    }

    /**
     * List row enrichment: sets the comma-separated `subSystemCodes` string field on [SysTenantRow].
     *
     * @param row list row to enrich
     * @author K
     * @since 1.0.0
     */
    private fun enrichTenantRow(row: SysTenantRow) {
        row.subSystemCodes = getSubSystemCodesString(row.id)
    }

    /**
     * Copy a [SysTenant] PO into a flat [SysTenantRow] VO for the list endpoint.
     *
     * @param tenant tenant PO
     * @return tenant VO (`subSystemCodes` is left blank and filled later by [enrichTenantRow])
     * @author K
     * @since 1.0.0
     */
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

    @Transactional(readOnly = true)
    override fun getSubSystemCodesFromCache(tenantId: String): Set<String> =
        sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId)

    /**
     * Flatten the sub-system codes bound to a tenant into a comma-separated string for direct display in list/detail views.
     *
     * @param tenantId tenant id
     * @return string like `"sys, msg, user"`; empty when there are no bindings
     * @author K
     * @since 1.0.0
     */
    private fun getSubSystemCodesString(tenantId: String): String =
        sysTenantSystemHashCache.getSubSystemCodesByTenantId(tenantId).joinToString(", ")
}
