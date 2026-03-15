package io.kudos.ms.sys.common.vo.system

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KProperty0


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

    override fun getReturnEntityClass() = SysSystemRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::name to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}
