package io.kudos.ms.sys.common.vo.system

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 系统查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemQuery (


    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否子系统 */
    val subSystem: Boolean? = null,

    /** 是否启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {


    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysSystemRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(
        ::code.name to OperatorEnum.ILIKE,
        ::name.name to OperatorEnum.ILIKE
    )


}
