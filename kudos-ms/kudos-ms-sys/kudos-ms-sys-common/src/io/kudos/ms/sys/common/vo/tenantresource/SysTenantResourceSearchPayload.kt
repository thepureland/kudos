package io.kudos.ms.sys.common.vo.tenantresource

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户-资源关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantResourceSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysTenantResourceRecord::class,

    /** 租户id */
    var tenantId: String? = null,

    /** 资源id */
    var resourceId: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysTenantResourceRecord::class)

    //endregion your codes 3

}