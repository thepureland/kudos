package io.kudos.ams.sys.common.vo.param

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
    override var id: String? = null,

    //region your codes 1

    /** 参数名称 */
    var paramName: String? = null,

    /** 参数值 */
    var paramValue: String? = null,

    /** 默认参数值 */
    var defaultValue: String? = null,

    /** 模块 */
    var moduleCode: String? = null,

    /** 序号 */
    var orderNum: Int? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建用户 */
    var createUser: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户 */
    var updateUser: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}