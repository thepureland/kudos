package io.kudos.ms.sys.common.vo.param

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 参数查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamDetail (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 参数名称 */
    val paramName: String? = null,

    /** 参数值 */
    val paramValue: String? = null,

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 模块 */
    val atomicServiceCode: String? = null,

    /** 序号 */
    val orderNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}