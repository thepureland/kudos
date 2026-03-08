package io.kudos.ms.sys.common.vo.tenantlocale

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户-语言关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantLocaleSearchPayload (

    //region your codes 1

    /** 租户id */
    val tenantId: String? = null,

    /** 语言代码 */
    val localeCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysTenantLocaleRecord::class

    //endregion your codes 3

}