package io.kudos.ms.sys.core.tenant.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem


/**
 * Tenant-system relationship service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysTenantSystemService : IBaseCrudService<String, SysTenantSystem> {


    /**
     * Returns the system codes associated with the given tenant.
     *
     * @param tenantId tenant id
     * @return Set<systemCode>
     */
    fun searchSystemCodesByTenantId(tenantId: String): Set<String>

    /**
     * Returns the tenant ids associated with the given system code.
     *
     * @param systemCode system code
     * @return Set<tenantId>
     */
    fun searchTenantIdsBySystemCode(systemCode: String): Set<String>

    /**
     * Groups system codes by tenant id.
     *
     * @param tenantIds tenant id collection; when null, queries all records (default null)
     * @return Map<tenantId, List<systemCode>>
     */
    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>>

    /**
     * Groups tenant ids by system code.
     *
     * @param systemCodes system code collection; when null, queries all records (default null)
     * @return Map<systemCode, List<tenantId>>
     */
    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>>

    /**
     * Batch binds the given systems to the tenant.
     *
     * @param tenantId tenant id
     * @param systemCodes system code collection
     * @return number of bindings successfully created
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, systemCodes: Collection<String>): Int

    /**
     * Unbinds the relationship between the tenant and the system.
     *
     * @param tenantId tenant id
     * @param systemCode system code
     * @return whether the unbind succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(tenantId: String, systemCode: String): Boolean

    /**
     * Checks whether the relationship exists.
     *
     * @param tenantId tenant id
     * @param systemCode system code
     * @return whether it exists
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(tenantId: String, systemCode: String): Boolean

    /**
     * Deletes all tenant-system relationships for the given tenant.
     *
     * @param tenantId tenant id
     * @return number of rows deleted
     */
    fun deleteByTenantId(tenantId: String): Int

    /**
     * Batch deletes tenant-system relationships for the given tenant ids.
     *
     * @param tenantIds tenant id collection
     * @return number of rows deleted
     */
    fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int


}
