package io.kudos.ms.sys.common.vo.subsystemmicroservice

import io.kudos.base.support.payload.FormPayload


/**
 * 子系统-微服务关系表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSubSystemMicroServicePayload (

    /**  */
    override var id: String? = null,

    //region your codes 1

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 微服务编码 */
    var microServiceCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}