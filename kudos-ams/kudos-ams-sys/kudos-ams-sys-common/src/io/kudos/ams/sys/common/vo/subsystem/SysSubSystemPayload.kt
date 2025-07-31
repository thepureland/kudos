package io.kudos.ams.sys.common.vo.subsystem

import io.kudos.base.support.payload.FormPayload


/**
 * 子系统表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSubSystemPayload (

    /** 编码 */
    override var id: String? = null,

    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** 门户编码 */
    var portalCode: String? = null,

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

    // endregion your codes 3

}