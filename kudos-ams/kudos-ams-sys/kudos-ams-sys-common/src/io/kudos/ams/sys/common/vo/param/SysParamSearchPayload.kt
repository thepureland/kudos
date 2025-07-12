package io.kudos.ams.sys.common.vo.param

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 参数查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysParamSearchPayload : ListSearchPayload() {
//endregion your codes 1

    //region your codes 2

    /** 模块 */
    var module: String? = null

    /** 参数名称 */
    var paramName: String? = null

    /** 参数值，或其国际化key */
    var paramValue: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    //endregion your codes 2

    override var returnEntityClass: KClass<*>? = SysParamRecord::class

}