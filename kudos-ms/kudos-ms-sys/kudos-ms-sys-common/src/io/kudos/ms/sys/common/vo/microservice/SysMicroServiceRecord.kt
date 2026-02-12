package io.kudos.ms.sys.common.vo.microservice

import io.kudos.base.support.result.IdJsonResult


/**
 * 微服务查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceRecord (

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

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var id: String
        get() = this.code ?: ""
        set(value) { this.code = value }

    // endregion your codes 3

}
