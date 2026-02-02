package io.kudos.ms.sys.common.vo.param

import io.kudos.base.support.payload.FormPayload


/**
 * 参数表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamPayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 参数名称 */
    var paramName: String? = null,

    /** 参数值 */
    var paramValue: String? = null,

    /** 默认参数值 */
    var defaultValue: String? = null,

    /** 模块 */
    var atomicServiceCode: String? = null,

    /** 序号 */
    var orderNum: Int? = null,

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