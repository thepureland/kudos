package io.kudos.ams.sys.common.vo.tenantlanguage

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户-语言关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantLanguageSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysTenantLanguageRecord::class,

    /** 租户id */
    var tenantId: String? = null,

    /** 语言代码 */
    var languageCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysTenantLanguageRecord::class)

    //endregion your codes 3

}