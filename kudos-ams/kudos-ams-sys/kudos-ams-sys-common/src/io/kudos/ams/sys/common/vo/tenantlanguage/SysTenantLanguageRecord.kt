package io.kudos.ams.sys.common.vo.tenantlanguage

import io.kudos.base.support.result.IdJsonResult


/**
 * 租户-语言关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantLanguageRecord (

    //region your codes 1

    /** 主键 */
    override var id: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 语言代码 */
    var languageCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}