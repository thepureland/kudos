package io.kudos.ams.sys.common.vo.portal

import io.kudos.base.support.payload.FormPayload


/**
 * 门户表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysPortalPayload (

    //region your codes 1

    /** 编码 */
    var code: String? = null,

    /** 名称 */
    var name: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    override var id: String?
        get() = this.code
        set(value) { this.code = value }

    // endregion your codes 3

}