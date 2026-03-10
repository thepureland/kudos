package io.kudos.ms.sys.common.vo.microservice

import io.kudos.base.support.result.IdJsonResult


/**
 * 微服务查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceRow (

    //region your codes 1

    override val id: String = "",

    /** 编码 */
    val code: String = "",

    /** 名称 */
    val name: String = "",

    /** 上下文 */
    val context: String = "",

    /** 是否为原子服务 */
    val atomicService: Boolean = true,

    /** 父服务编码 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
