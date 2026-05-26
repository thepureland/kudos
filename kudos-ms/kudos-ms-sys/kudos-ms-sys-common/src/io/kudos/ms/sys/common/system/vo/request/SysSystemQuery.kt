package io.kudos.ms.sys.common.system.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.system.vo.response.SysSystemRow
import kotlin.reflect.KProperty0


/**
 * System list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemQuery (

    /** Code */
    val code: String? = null,

    /** Name */
    val name: String? = null,

    /** Whether sub-system */
    val subSystem: Boolean? = null,

    /** Whether active */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysSystemRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::name to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}