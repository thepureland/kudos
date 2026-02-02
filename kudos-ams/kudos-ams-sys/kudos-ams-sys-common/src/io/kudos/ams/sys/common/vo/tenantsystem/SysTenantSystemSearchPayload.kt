package io.kudos.ams.sys.common.vo.tenantsystem

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户-系统关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSystemSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysTenantSystemRecord::class,

    /** 租户id */
    var tenantId: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysTenantSystemRecord::class)

    //endregion your codes 3

}
