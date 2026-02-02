package io.kudos.ms.sys.common.vo.param

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 参数查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysParamRecord::class,

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

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysParamRecord::class)

    //endregion your codes 3

}