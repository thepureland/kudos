package io.kudos.ms.sys.common.vo.param

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 参数查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamQuery (

    //region your codes 1

    /** 参数名称 */
    val paramName: String? = null,

    /** 参数值 */
    val paramValue: String? = null,

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 模块 */
    val atomicServiceCode: String? = null,

    /** 仅启用 */
    val active: Boolean? = true,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysParamRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(
        ::paramName.name to OperatorEnum.ILIKE,
        ::paramValue.name to OperatorEnum.LIKE_S
    )

    //endregion your codes 3

}