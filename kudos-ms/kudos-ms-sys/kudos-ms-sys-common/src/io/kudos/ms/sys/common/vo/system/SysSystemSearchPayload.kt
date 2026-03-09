package io.kudos.ms.sys.common.vo.system

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 系统查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemSearchPayload (

    //region your codes 1

    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否子系统 */
    val subSystem: Boolean? = null,

    /** 是否启用 */
    val active: Boolean? = true,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysSystemRecord::class

    //endregion your codes 3

}
