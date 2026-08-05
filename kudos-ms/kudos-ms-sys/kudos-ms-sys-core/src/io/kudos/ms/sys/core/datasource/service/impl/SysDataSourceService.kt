package io.kudos.ms.sys.core.datasource.service.impl

import io.kudos.ms.sys.core.platform.service.impl.completeCrudInsert
import io.kudos.ms.sys.core.platform.service.impl.completeCrudUpdate
import io.kudos.ms.sys.core.platform.service.impl.requireStringId

import io.kudos.base.support.service.impl.BaseCrudService
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.PagingSearchResult
import io.kudos.base.query.eq
import io.kudos.base.security.CryptoKit
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.tenant.api.ISysTenantApi
import io.kudos.ms.sys.common.datasource.consts.SysDataSourceConsts
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.common.datasource.vo.request.SysDataSourceFormUpdate
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
 * Data source service.
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

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    override fun <R : Any> get(id: String, returnType: KClass<R>): R? =
        if (returnType == SysDataSourceCacheEntry::class) sysDataSourceHashCache.getDataSourceById(id) as R?
        else enrichDataSourceDetail(super.get(id, returnType), returnType)

    @Transactional
    override fun insert(any: Any): String {
        val id = super.insert(any)
        completeCrudInsert(log, "Inserted data source id=$id.") {
            eventPublisher.publishEvent(SysDataSourceInserted(id = id))
        }
        return id
    }

    @Transactional
    override fun update(any: Any): Boolean {
        val payload = resolveUpdatePayload(any)
        val id = requireStringId(payload, "data source")
        return completeCrudUpdate(
            success = super.update(payload),
            log = log,
            successMessage = "Updated data source id=$id.",
            failureMessage = "Failed to update data source id=$id!",
        ) {
            eventPublisher.publishEvent(SysDataSourceUpdated(id = id))
        }
    }

    /**
     * Edit forms round-trip with a masked password (admin responses replace the stored ciphertext with
     * [SysDataSourceConsts.PASSWORD_MASK]), so a blank or masked password in [SysDataSourceFormUpdate]
     * means "keep the stored password unchanged". This substitutes the currently persisted password back
     * into the payload before updating, preventing the mask from ever being written to the database.
     * Real password changes go through [resetPassword] (which encrypts), not this path.
     *
     * @param any update payload as received by [update]
     * @return payload safe to persist
     * @author K
     * @author AI: Claude
     * @since 1.0.0
     */
    private fun resolveUpdatePayload(any: Any): Any =
        if (any is SysDataSourceFormUpdate && shouldKeepStoredPassword(any.password)) {
            any.copy(password = dao.get(any.id)?.password)
        } else any

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val dataSource = SysDataSource {
            this.id = id
            this.active = active
        }
        return completeCrudUpdate(
            success = dao.update(dataSource),
            log = log,
            successMessage = "Updated data source id=$id active=$active.",
            failureMessage = "Failed to update data source id=$id active=$active!",
        ) {
            eventPublisher.publishEvent(SysDataSourceUpdated(id = id))
        }
    }

    @Transactional
    override fun resetPassword(id: String, newPassword: String) {
        val newPwd = CryptoKit.aesEncrypt(newPassword) // encrypt password
        val dataSource = SysDataSource {
            this.id = id
            this.password = newPwd
        }
        completeCrudUpdate(
            success = dao.update(dataSource),
            log = log,
            successMessage = "Reset password for data source id=$id.",
            failureMessage = "Failed to reset password for data source id=$id!",
        ) {
            eventPublisher.publishEvent(SysDataSourceUpdated(id = id))
        }
    }

    @Transactional
    override fun deleteById(id: String): Boolean {
        val existing = dao.get(id)
        if (existing == null) {
            log.warn("Data source id=$id no longer exists when attempting delete!")
            return false
        }
        return completeCrudUpdate(
            success = super.deleteById(id),
            log = log,
            successMessage = "Deleted data source id=$id.",
            failureMessage = "Failed to delete data source id=$id!",
        ) {
            eventPublisher.publishEvent(SysDataSourceDeleted(id = id))
        }
    }

    @Transactional
    override fun batchDelete(ids: Collection<String>): Int {
        val count = super.batchDelete(ids)
        log.debug("Batch delete data sources: expected ${ids.size}, actually deleted $count.")
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

    override fun testConnection(url: String, username: String, password: String?): Boolean =
        runCatching {
            io.kudos.ability.data.rdb.jdbc.kit.RdbKit.newConnection(url, username, password).use { conn ->
                io.kudos.ability.data.rdb.jdbc.kit.RdbKit.testConnection(conn)
            }
        }.onFailure { log.warn("Data source connectivity test failed url=$url username=$username: ${it.message}") }
            .getOrDefault(false)

    /**
     * List enrichment: batch-load tenantName from the tenant cache to avoid per-row queries.
     * Empty lists short-circuit so we never pass an empty collection to the cache layer.
     *
     * @param records data source rows to enrich (modified in place)
     * @author K
     * @since 1.0.0
     */
    private fun enrichTenantNames(records: List<SysDataSourceRow>) {
        if (records.isEmpty()) return
        val tenants = sysTenantApi.getTenantsFromCacheByIds(records.mapNotNull { it.tenantId })
        records.forEach { record ->
            record.tenantName = tenants[record.tenantId]?.name
        }
    }

    /**
     * Detail enrichment: only fills `tenantName` when returnType is [SysDataSourceDetail];
     * other return types pass through unchanged so no unrelated fields are introduced.
     *
     * @param R return type
     * @param result object to enrich
     * @param returnType expected return type
     * @return enriched object (unchanged when type does not match)
     * @author K
     * @since 1.0.0
     */
    private fun <R : Any> enrichDataSourceDetail(result: R?, returnType: KClass<R>): R? {
        if (returnType == SysDataSourceDetail::class && result is SysDataSourceDetail) {
            result.tenantName = result.tenantId
                ?.takeUnless(String::isBlank)
                ?.let { sysTenantApi.getTenantFromCache(it)?.name }
        }
        return result
    }

}

/**
 * Whether the password submitted on an update form means "do not change the stored password":
 * `null`, blank, or exactly the output mask [SysDataSourceConsts.PASSWORD_MASK] all qualify,
 * since admin responses never expose the real (encrypted) password and edit forms round-trip the mask.
 *
 * Exposed as `internal` purely for unit testing.
 *
 * @param password password field of the update form
 * @return `true` when the stored password should be kept as is
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal fun shouldKeepStoredPassword(password: String?): Boolean =
    password.isNullOrBlank() || password == SysDataSourceConsts.PASSWORD_MASK
