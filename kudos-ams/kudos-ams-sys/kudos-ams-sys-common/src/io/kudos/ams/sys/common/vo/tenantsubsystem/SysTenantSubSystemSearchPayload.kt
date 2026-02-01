package io.kudos.ams.sys.common.vo.tenantsubsystem

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户-子系统关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSubSystemSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysTenantSubSystemRecord::class,

    /** 租户id */
    var tenantId: String? = null,

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysTenantSubSystemRecord::class)

    //endregion your codes 3

}