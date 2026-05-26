package io.kudos.ms.sys.common.param.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import kotlin.reflect.KProperty0


/**
 * Parameter list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamQuery (

    /** Parameter name */
    val paramName: String? = null,

    /** Parameter value */
    val paramValue: String? = null,

    /** Default parameter value */
    val defaultValue: String? = null,

    /** Module */
    val atomicServiceCode: String? = null,

    /** Active only */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysParamRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::paramName to OperatorEnum.ILIKE,
        ::paramValue to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}