package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.support.payload.FormPayload


/**
 * 租户表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantPayload (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** 时区 */
    var timezone: String? = null,

    /** 默认语言编码 */
    var defaultLanguageCode: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}