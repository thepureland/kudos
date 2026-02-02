package io.kudos.ms.sys.common.vo.microservice

import io.kudos.base.support.payload.FormPayload


/**
 * 微服务表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServicePayload (

    //region your codes 1

    /** 编码 */
    var code: String? = null,

    /** 名称 */
    var name: String? = null,

    /** 上下文 */
    var context: String? = null,

    /** 是否为原子服务 */
    var atomicService: Boolean? = null,

    /** 父服务编码 */
    var parentCode: String? = null,

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
