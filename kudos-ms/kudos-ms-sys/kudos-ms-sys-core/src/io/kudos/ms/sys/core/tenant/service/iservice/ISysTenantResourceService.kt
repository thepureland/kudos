package io.kudos.ms.sys.core.tenant.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.core.tenant.model.po.SysTenantResource


/**
 * Tenant-resource relationship service interface.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantResourceService : IBaseCrudService<String, SysTenantResource> {


    /**
     * Returns the resource id list for the given tenant.
     *
     * @param tenantId tenant id
     * @return resource id set
     * @author K
     * @since 1.0.0
     */
    fun getResourceIdsByTenantId(tenantId: String): Set<String>

    /**
     * Returns the tenant id list for the given resource.
     *
     * @param resourceId resource id
     * @return tenant id set
     * @author K
     * @since 1.0.0
     */
    fun getTenantIdsByResourceId(resourceId: String): Set<String>

    /**
     * Batch binds the given resources to the tenant.
     *
     * @param tenantId tenant id
     * @param resourceIds resource id collection
     * @return number of bindings successfully created
     * @author K
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, resourceIds: Collection<String>): Int

    /**
     * Unbinds the relationship between the tenant and the resource.
     *
     * @param tenantId tenant id
     * @param resourceId resource id
     * @return whether the unbind succeeded
     * @author K
     * @since 1.0.0
     */
    fun unbind(tenantId: String, resourceId: String): Boolean

    /**
     * Checks whether the relationship exists.
     *
     * @param tenantId tenant id
     * @param resourceId resource id
     * @return whether it exists
     * @author K
     * @since 1.0.0
     */
    fun exists(tenantId: String, resourceId: String): Boolean


}