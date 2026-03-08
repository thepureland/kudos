package io.kudos.ms.sys.common.vo.subsystemmicroservice

import io.kudos.base.support.result.IdJsonResult


/**
 * 子系统-微服务关系查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysSubSystemMicroServiceRecord (

    //region your codes 1

    /** 主键 */
    override val id: String = "",

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 微服务编码 */
    val microServiceCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}