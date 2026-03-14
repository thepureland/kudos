package io.kudos.ms.sys.common.api


/**
 * 租户-资源关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysTenantResourceApi {


    fun getResourceIdsByTenantId(tenantId: String): Set<String>

    fun getTenantIdsByResourceId(resourceId: String): Set<String>

    fun batchBind(tenantId: String, resourceIds: Collection<String>): Int

    fun unbind(tenantId: String, resourceId: String): Boolean

    fun exists(tenantId: String, resourceId: String): Boolean


}