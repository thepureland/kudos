package io.kudos.ms.sys.common.vo.system

import io.kudos.base.support.payload.FormPayload


/**
 * 系统表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemPayload (

    //region your codes 1

    override val id: String = "",

    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否子系统 */
    val subSystem: Boolean? = null,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
