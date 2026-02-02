package io.kudos.ms.sys.common.vo.tenantlanguage

import io.kudos.base.support.payload.FormPayload


/**
 * 租户-语言关系表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantLanguagePayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 租户id */
    var tenantId: String? = null,

    /** 语言代码 */
    var languageCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}