package io.kudos.ms.sys.common.api


/**
 * 租户-资源关系 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantResourceApi {
//endregion your codes 1

    //region your codes 2

    fun getResourceIdsByTenantId(tenantId: String): Set<String>

    fun getTenantIdsByResourceId(resourceId: String): Set<String>

    fun batchBind(tenantId: String, resourceIds: Collection<String>): Int

    fun unbind(tenantId: String, resourceId: String): Boolean

    fun exists(tenantId: String, resourceId: String): Boolean

    //endregion your codes 2

}